package com.example.ui.components

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.ProjectTaskEntity
import com.example.data.model.TaskPriority
import com.example.data.model.TaskStatus
import com.example.ui.theme.*
import com.example.util.tr
import com.example.util.trFmt
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * OOZOX-Branded Project Task Assignment Dialog.
 * Replaces generic Material 3 AlertDialog defaults with unified OOZOX Teal surfaces,
 * rounded pill selectors that prevent vertical letter wrapping on RTL priority chips,
 * and full dark/light theme contrast compliance.
 */
@Composable
fun ProjectTaskAssignDialog(
    projectId: String,
    projectName: String,
    workGroupId: String?,
    onDismiss: () -> Unit,
    onAssignTask: (ProjectTaskEntity) -> Unit,
    onShowValidationError: (String) -> Unit
) {
    var taskTitle by remember { mutableStateOf("") }
    var taskDesc by remember { mutableStateOf("") }
    var taskAssignee by remember { mutableStateOf("") }
    var taskCost by remember { mutableStateOf("") }
    var taskPriority by remember { mutableStateOf(TaskPriority.MEDIUM.name) }
    var taskTimingMode by remember { mutableStateOf("DAYS") } // NOW, HOURS, DAYS
    var taskHoursText by remember { mutableStateOf("6") }
    var taskDaysText by remember { mutableStateOf("3") }

    val scrollState = rememberScrollState()
    val isDark = isDarkThemeActive
    val brandAccent = if (isDark) OozoxTealLight else GoldDark
    val brandCtaContainer = if (isDark) OozoxTeal else GoldDark
    val brandCtaContent = if (isDark) OozoxObsidian else Color.White

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = brandAccent,
        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
        focusedLabelColor = brandAccent,
        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        cursorColor = brandAccent,
        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f),
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.14f)
    )

    val calculatedDurationMs: Long = when (taskTimingMode) {
        "NOW" -> 15L * 60L * 1000L
        "HOURS" -> ((taskHoursText.toIntOrNull() ?: 1).coerceIn(1, 720)).toLong() * 3600L * 1000L
        else -> ((taskDaysText.toIntOrNull() ?: 1).coerceIn(1, 365)).toLong() * 24L * 3600L * 1000L
    }
    val calculatedDurationNum: Int = when (taskTimingMode) {
        "NOW" -> 0
        "HOURS" -> (taskHoursText.toIntOrNull() ?: 1).coerceIn(1, 720)
        else -> (taskDaysText.toIntOrNull() ?: 1).coerceIn(1, 365)
    }
    val calculatedDurationText: String = when (taskTimingMode) {
        "NOW" -> tr("task.mode_now")
        "HOURS" -> trFmt("task.within_hours_fmt", "count" to calculatedDurationNum.toString())
        else -> trFmt("task.within_days_fmt", "count" to calculatedDurationNum.toString())
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp)
                    .testTag("project_task_assign_dialog"),
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
                shadowElevation = 16.dp,
                border = BorderStroke(1.dp, brandAccent.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 16.dp),
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
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = brandAccent.copy(alpha = 0.14f),
                                border = BorderStroke(1.dp, brandAccent.copy(alpha = 0.35f)),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.AddTask,
                                        contentDescription = null,
                                        tint = brandAccent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "إسناد مهمة جديدة للمشروع",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.5.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = projectName,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = brandAccent,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        OozoxCircleIconButton(
                            onClick = onDismiss,
                            imageVector = Icons.Default.Close,
                            contentDescription = tr("common.close"),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            modifier = Modifier.testTag("close_add_task_dialog_btn")
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                    // Scrollable Form Body
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .verticalScroll(scrollState),
                        verticalArrangement = Arrangement.spacedBy(11.dp)
                    ) {
                        OutlinedTextField(
                            value = taskTitle,
                            onValueChange = { taskTitle = it },
                            label = { Text("عنوان المهمة / البند", fontSize = 12.5.sp) },
                            placeholder = { Text("توريد خامات / مراجعة هندسية", fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Assignment,
                                    contentDescription = null,
                                    tint = brandAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = fieldColors,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("quick_task_title_input")
                        )

                        OutlinedTextField(
                            value = taskDesc,
                            onValueChange = { taskDesc = it },
                            label = { Text("تفاصيل ومواصفات التنفيذ (اختياري)", fontSize = 12.5.sp) },
                            minLines = 2,
                            maxLines = 3,
                            shape = RoundedCornerShape(14.dp),
                            colors = fieldColors,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = taskAssignee,
                            onValueChange = { taskAssignee = it },
                            label = { Text("اسم المكلف / الموظف المسؤول", fontSize = 12.5.sp) },
                            placeholder = { Text("م. أحمد", fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.PersonOutline,
                                    contentDescription = null,
                                    tint = brandAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = fieldColors,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Timing Mode Selector Card
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.26f),
                            border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = brandAccent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = tr("task.timing_mode_label"),
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = brandAccent
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    TaskModeOptionPill(
                                        label = tr("task.mode_now"),
                                        icon = Icons.Default.FlashOn,
                                        selected = taskTimingMode == "NOW",
                                        accentColor = adaptiveStatusRed,
                                        onClick = {
                                            taskTimingMode = "NOW"
                                            taskPriority = TaskPriority.URGENT.name
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                    TaskModeOptionPill(
                                        label = tr("task.mode_hours"),
                                        icon = Icons.Default.Timer,
                                        selected = taskTimingMode == "HOURS",
                                        accentColor = brandAccent,
                                        onClick = { taskTimingMode = "HOURS" },
                                        modifier = Modifier.weight(1f)
                                    )
                                    TaskModeOptionPill(
                                        label = tr("task.mode_days"),
                                        icon = Icons.Default.DateRange,
                                        selected = taskTimingMode == "DAYS",
                                        accentColor = brandAccent,
                                        onClick = { taskTimingMode = "DAYS" },
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                if (taskTimingMode == "HOURS") {
                                    OutlinedTextField(
                                        value = taskHoursText,
                                        onValueChange = { taskHoursText = it.filter { c -> c.isDigit() }.take(3) },
                                        label = { Text(tr("task.custom_hours_label"), fontSize = 12.sp) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        colors = fieldColors,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf(2, 6, 12, 24, 48).forEach { preset ->
                                            DurationPresetChip(
                                                label = "$preset${tr("task.hour_short")}",
                                                selected = taskHoursText == preset.toString(),
                                                accentColor = brandAccent,
                                                onClick = { taskHoursText = preset.toString() }
                                            )
                                        }
                                    }
                                } else if (taskTimingMode == "DAYS") {
                                    OutlinedTextField(
                                        value = taskDaysText,
                                        onValueChange = { taskDaysText = it.filter { c -> c.isDigit() }.take(3) },
                                        label = { Text(tr("task.custom_days_label"), fontSize = 12.sp) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        colors = fieldColors,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf(1, 3, 7, 14, 30).forEach { preset ->
                                            DurationPresetChip(
                                                label = "$preset${tr("task.day_short")}",
                                                selected = taskDaysText == preset.toString(),
                                                accentColor = brandAccent,
                                                onClick = { taskDaysText = preset.toString() }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = taskCost,
                            onValueChange = { taskCost = it.filter { c -> c.isDigit() || c == '.' } },
                            label = { Text("التكلفة التقديرية أو الميزانية", fontSize = 12.5.sp) },
                            placeholder = { Text("0.0", fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.AttachMoney,
                                    contentDescription = null,
                                    tint = brandAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = fieldColors,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Priority Selector (2x2 Balanced Grid — guarantees no vertical letter-wrapping)
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "الأولوية:",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                PriorityOptionPill(
                                    label = "منخفضة",
                                    accentColor = adaptiveStatusGreen,
                                    selected = taskPriority == TaskPriority.LOW.name,
                                    onClick = { taskPriority = TaskPriority.LOW.name },
                                    modifier = Modifier.weight(1f)
                                )
                                PriorityOptionPill(
                                    label = "متوسطة",
                                    accentColor = brandAccent,
                                    selected = taskPriority == TaskPriority.MEDIUM.name,
                                    onClick = { taskPriority = TaskPriority.MEDIUM.name },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                PriorityOptionPill(
                                    label = "عالية",
                                    accentColor = adaptiveStatusOrange,
                                    selected = taskPriority == TaskPriority.HIGH.name,
                                    onClick = { taskPriority = TaskPriority.HIGH.name },
                                    modifier = Modifier.weight(1f)
                                )
                                PriorityOptionPill(
                                    label = "طوارئ",
                                    accentColor = adaptiveStatusRed,
                                    selected = taskPriority == TaskPriority.URGENT.name,
                                    onClick = { taskPriority = TaskPriority.URGENT.name },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

                    // Footer Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text(
                                text = tr("common.cancel"),
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        Button(
                            onClick = {
                                if (taskTitle.isNotBlank()) {
                                    val cost = taskCost.toDoubleOrNull() ?: 0.0
                                    val deadlineTs = System.currentTimeMillis() + calculatedDurationMs
                                    val dueFmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(deadlineTs))
                                    val assigneeName = taskAssignee.trim().ifBlank { "فريق المشروع" }
                                    val task = ProjectTaskEntity(
                                        id = "proj_task_${System.currentTimeMillis()}",
                                        projectId = projectId,
                                        projectName = projectName,
                                        title = taskTitle.trim(),
                                        description = taskDesc.trim().ifBlank { null },
                                        assignedToUserId = null,
                                        assignedToName = assigneeName,
                                        assignedToPhone = null,
                                        priority = if (taskTimingMode == "NOW") TaskPriority.URGENT.name else taskPriority,
                                        status = TaskStatus.PENDING.name,
                                        dueDate = dueFmt,
                                        costEstimate = cost,
                                        isSynced = true,
                                        workGroupId = workGroupId,
                                        assignedMembersList = assigneeName,
                                        deadlineDurationText = calculatedDurationText,
                                        deadlineTimestamp = deadlineTs,
                                        urgencyAlertLevel = if (taskTimingMode == "NOW") "CRITICAL" else "NORMAL",
                                        financialAmount = cost,
                                        timingMode = taskTimingMode,
                                        durationValue = calculatedDurationNum
                                    )
                                    onAssignTask(task)
                                } else {
                                    onShowValidationError("يرجى كتابة عنوان المهمة")
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = brandCtaContainer,
                                contentColor = brandCtaContent
                            ),
                            modifier = Modifier
                                .weight(1.35f)
                                .height(48.dp)
                                .testTag("confirm_quick_task_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = brandCtaContent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "إسناد المهمة",
                                color = brandCtaContent,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskModeOptionPill(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (selected) accentColor.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (selected) 1.3.dp else 0.8.dp,
            color = if (selected) accentColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
        ),
        modifier = modifier
            .heightIn(min = 42.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DurationPresetChip(
    label: String,
    selected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (selected) accentColor.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (selected) 1.2.dp else 0.8.dp,
            color = if (selected) accentColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier
            .heightIn(min = 36.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = 11.5.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
private fun PriorityOptionPill(
    label: String,
    accentColor: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (selected) accentColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f),
        border = BorderStroke(
            width = if (selected) 1.4.dp else 0.8.dp,
            color = if (selected) accentColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        modifier = modifier
            .heightIn(min = 42.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (selected) accentColor else accentColor.copy(alpha = 0.45f))
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) accentColor else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
