package com.example.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.ProjectTaskEntity
import com.example.data.model.TaskPriority
import com.example.data.model.TaskStatus
import com.example.ui.theme.*
import com.example.util.GoldUtils
import java.text.SimpleDateFormat
import java.util.*

/**
 * Dialog for Admin / Manager to create a Physical Field Operation Task
 * with photo/video attachment, team member mentions, and flexible timing modes:
 * NOW (immediate), HOURS (1..720h), or DAYS (1..365d).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePhysicalFieldTaskDialog(
    projectId: String,
    projectName: String,
    workGroupId: String? = null,
    availableMembers: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onCreateTask: (ProjectTaskEntity) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedMediaType by remember { mutableStateOf("PHOTO") }
    var mediaUri by remember { mutableStateOf<String?>(null) }
    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) mediaUri = uri.toString()
    }
    var financialAmountStr by remember { mutableStateOf("") }
    var financialNotes by remember { mutableStateOf("") }

    val selectedMembers = remember { mutableStateListOf<String>() }
    var customMemberInput by remember { mutableStateOf("") }

    // Timing Modes: NOW, HOURS, DAYS (no 4-hour cap!)
    var timingMode by remember { mutableStateOf("DAYS") } // NOW, HOURS, DAYS
    var hoursValueText by remember { mutableStateOf("6") }
    var daysValueText by remember { mutableStateOf("3") }
    var selectedPriority by remember { mutableStateOf(TaskPriority.HIGH.name) }

    val suggestedMembers = remember(availableMembers) {
        if (availableMembers.isNotEmpty()) availableMembers else listOf(
            "@فريق العمل الكامل",
            "@مدير الموقع الميداني",
            "@مشرف التشغيل والتنفيذ"
        )
    }

    val durationMillis: Long = when (timingMode) {
        "NOW" -> 15L * 60L * 1000L // Urgent immediate window (15 mins tracking baseline)
        "HOURS" -> {
            val h = (hoursValueText.toIntOrNull() ?: 1).coerceIn(1, 720)
            h.toLong() * 3600L * 1000L
        }
        else -> {
            val d = (daysValueText.toIntOrNull() ?: 1).coerceIn(1, 365)
            d.toLong() * 24L * 3600L * 1000L
        }
    }
    val numericDuration: Int = when (timingMode) {
        "NOW" -> 0
        "HOURS" -> (hoursValueText.toIntOrNull() ?: 1).coerceIn(1, 720)
        else -> (daysValueText.toIntOrNull() ?: 1).coerceIn(1, 365)
    }
    val deadlineLabelText: String = when (timingMode) {
        "NOW" -> com.example.util.tr("task.mode_now")
        "HOURS" -> com.example.util.trFmt("task.within_hours_fmt", "count" to numericDuration.toString())
        else -> com.example.util.trFmt("task.within_days_fmt", "count" to numericDuration.toString())
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.91f),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 12.dp,
            border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.45f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            shape = CircleShape,
                            color = GoldPrimary.copy(alpha = 0.15f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.AssignmentInd, contentDescription = null, tint = GoldPrimary)
                            }
                        }
                        Column {
                            Text(
                                text = com.example.util.tr("hr.assign_task_group"),
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = projectName,
                                fontSize = 11.5.sp,
                                color = OozoxTeal
                            )
                        }
                    }
                    OozoxCircleIconButton(
                        onClick = onDismiss,
                        imageVector = Icons.Default.Close,
                        contentDescription = com.example.util.tr("common.close")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 1. Media Attachment Section
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "١. توثيق صورة أو فيديو العمل المطلوب (اختياري):",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldPrimary
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FilterChip(
                                        selected = selectedMediaType == "PHOTO",
                                        onClick = {
                                            selectedMediaType = "PHOTO"
                                            mediaPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        },
                                        label = { Text("📷 إرفاق صورة للمهمة", fontSize = 11.5.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = GoldPrimary,
                                            selectedLabelColor = Color.Black
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                    FilterChip(
                                        selected = selectedMediaType == "VIDEO",
                                        onClick = {
                                            selectedMediaType = "VIDEO"
                                            mediaPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                            )
                                        },
                                        label = { Text("🎥 إرفاق فيديو قصير", fontSize = 11.5.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = GoldPrimary,
                                            selectedLabelColor = Color.Black
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                if (mediaUri != null) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF10B981).copy(alpha = 0.1f),
                                        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Icon(
                                                    if (selectedMediaType == "VIDEO") Icons.Default.Videocam else Icons.Default.PhotoCamera,
                                                    contentDescription = null,
                                                    tint = Color(0xFF10B981),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Text(
                                                    text = if (selectedMediaType == "VIDEO") "تم إرفاق الفيديو" else "تم إرفاق الصورة",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                            Text("✓", fontSize = 11.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 2. Task Title & Description
                    item {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("عنوان المهمة المطلوبة", fontSize = 12.sp) },
                            placeholder = { Text("مثال: استلام وتوريد خامات / مراجعة حسابات الفرع") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("field_task_title_input")
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("النص التفصيلي والتعليمات المطلوبة", fontSize = 12.sp) },
                            placeholder = { Text("اكتب بدقة العمل المطلوب تنفيذه ومعايير التسليم...") },
                            minLines = 3,
                            maxLines = 5,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("field_task_desc_input")
                        )
                    }

                    // 3. Assign Team Members (unlimited selection + Select All)
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "٢. إسناد المهمة للموظفين بالمنشن (${selectedMembers.size}):",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GoldPrimary
                                    )
                                    TextButton(
                                        onClick = {
                                            if (selectedMembers.size == suggestedMembers.size) {
                                                selectedMembers.clear()
                                            } else {
                                                selectedMembers.clear()
                                                selectedMembers.addAll(suggestedMembers)
                                            }
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (selectedMembers.size == suggestedMembers.size) "إلغاء الكل" else "تحديد الكل",
                                            fontSize = 11.sp,
                                            color = OozoxTeal
                                        )
                                    }
                                }

                                if (selectedMembers.isNotEmpty()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        selectedMembers.forEach { member ->
                                            InputChip(
                                                selected = true,
                                                onClick = { selectedMembers.remove(member) },
                                                label = { Text(member, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                                trailingIcon = { Icon(Icons.Default.Close, contentDescription = com.example.util.tr("common.delete"), modifier = Modifier.size(14.dp)) },
                                                colors = InputChipDefaults.inputChipColors(
                                                    selectedContainerColor = GoldPrimary.copy(alpha = 0.2f),
                                                    selectedLabelColor = GoldAccent
                                                )
                                            )
                                        }
                                    }
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    suggestedMembers.forEach { member ->
                                        val isChosen = selectedMembers.contains(member)
                                        Surface(
                                            onClick = {
                                                if (isChosen) selectedMembers.remove(member)
                                                else selectedMembers.add(member)
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isChosen) GoldPrimary.copy(alpha = 0.15f) else Color.Transparent,
                                            border = BorderStroke(0.6.dp, if (isChosen) GoldPrimary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Checkbox(
                                                    checked = isChosen,
                                                    onCheckedChange = { chk ->
                                                        if (chk && !selectedMembers.contains(member)) selectedMembers.add(member)
                                                        else if (!chk) selectedMembers.remove(member)
                                                    },
                                                    colors = CheckboxDefaults.colors(checkedColor = GoldPrimary),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(member, fontSize = 11.5.sp)
                                            }
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    OutlinedTextField(
                                        value = customMemberInput,
                                        onValueChange = { customMemberInput = it },
                                        placeholder = { Text("أو أضف موظف/منشن: @الاسم...", fontSize = 11.sp) },
                                        singleLine = true,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f)
                                    )
                                    Button(
                                        onClick = {
                                            if (customMemberInput.isNotBlank()) {
                                                val formatted = if (customMemberInput.startsWith("@")) customMemberInput.trim() else "@${customMemberInput.trim()}"
                                                if (!selectedMembers.contains(formatted)) {
                                                    selectedMembers.add(formatted)
                                                }
                                                customMemberInput = ""
                                            }
                                        },
                                        enabled = customMemberInput.isNotBlank(),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text("إضافة", fontSize = 11.5.sp)
                                    }
                                }
                            }
                        }
                    }

                    // 4. Flexible Timing Mode (NOW / HOURS / DAYS) — NO 4-hour restriction!
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.Schedule, contentDescription = null, tint = OozoxTeal, modifier = Modifier.size(18.dp))
                                    Text(
                                        text = "٣. ${com.example.util.tr("task.timing_mode_label")}:",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GoldPrimary
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf(
                                        "NOW" to com.example.util.tr("task.mode_now"),
                                        "HOURS" to com.example.util.tr("task.mode_hours"),
                                        "DAYS" to com.example.util.tr("task.mode_days")
                                    ).forEach { (modeKey, modeLabel) ->
                                        FilterChip(
                                            selected = timingMode == modeKey,
                                            onClick = {
                                                timingMode = modeKey
                                                if (modeKey == "NOW") selectedPriority = TaskPriority.URGENT.name
                                            },
                                            label = {
                                                Text(
                                                    text = modeLabel,
                                                    fontSize = 11.sp,
                                                    fontWeight = if (timingMode == modeKey) FontWeight.Bold else FontWeight.Normal,
                                                    maxLines = 1
                                                )
                                            },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = if (modeKey == "NOW") Color(0xFFEF4444) else OozoxTeal,
                                                selectedLabelColor = Color.White
                                            ),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }

                                if (timingMode == "HOURS") {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = hoursValueText,
                                            onValueChange = { raw ->
                                                val digits = raw.filter { it.isDigit() }.take(3)
                                                hoursValueText = digits
                                            },
                                            label = { Text(com.example.util.tr("task.custom_hours_label"), fontSize = 11.sp) },
                                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                                            ),
                                            singleLine = true,
                                            modifier = Modifier.weight(1f).testTag("task_hours_input")
                                        )
                                        listOf(2, 6, 12, 24, 48).forEach { preset ->
                                            val isSelected = hoursValueText == preset.toString()
                                            FilterChip(
                                                selected = isSelected,
                                                onClick = { hoursValueText = preset.toString() },
                                                label = { Text("$preset${com.example.util.tr("task.hour_short")}", fontSize = 10.5.sp, maxLines = 1, softWrap = false) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = OozoxTeal.copy(alpha = 0.18f),
                                                    selectedLabelColor = if (isDarkThemeActive) OozoxTealLight else GoldDark
                                                ),
                                                border = FilterChipDefaults.filterChipBorder(
                                                    enabled = true,
                                                    selected = isSelected,
                                                    selectedBorderColor = OozoxTeal
                                                )
                                            )
                                        }
                                    }
                                } else if (timingMode == "DAYS") {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = daysValueText,
                                            onValueChange = { raw ->
                                                val digits = raw.filter { it.isDigit() }.take(3)
                                                daysValueText = digits
                                            },
                                            label = { Text(com.example.util.tr("task.custom_days_label"), fontSize = 11.sp) },
                                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                                            ),
                                            singleLine = true,
                                            modifier = Modifier.weight(1f).testTag("task_days_input")
                                        )
                                        listOf(1, 3, 7, 14, 30).forEach { preset ->
                                            val isSelected = daysValueText == preset.toString()
                                            FilterChip(
                                                selected = isSelected,
                                                onClick = { daysValueText = preset.toString() },
                                                label = { Text("$preset${com.example.util.tr("task.day_short")}", fontSize = 10.5.sp, maxLines = 1, softWrap = false) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = OozoxTeal.copy(alpha = 0.18f),
                                                    selectedLabelColor = if (isDarkThemeActive) OozoxTealLight else GoldDark
                                                ),
                                                border = FilterChipDefaults.filterChipBorder(
                                                    enabled = true,
                                                    selected = isSelected,
                                                    selectedBorderColor = OozoxTeal
                                                )
                                            )
                                        }
                                    }
                                }

                                val deadlineDate = Date(System.currentTimeMillis() + durationMillis)
                                val deadlineFormatted = SimpleDateFormat("yyyy-MM-dd  hh:mm a", Locale.getDefault()).format(deadlineDate)

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (timingMode == "NOW") Color(0xFFEF4444).copy(alpha = 0.12f) else OozoxTeal.copy(alpha = 0.12f),
                                    border = BorderStroke(1.dp, if (timingMode == "NOW") Color(0xFFEF4444).copy(alpha = 0.4f) else OozoxTeal.copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = if (timingMode == "NOW") Color(0xFFEF4444) else OozoxTeal, modifier = Modifier.size(16.dp))
                                            Text(
                                                text = if (timingMode == "NOW") com.example.util.tr("task.immediate_tracking_msg") else com.example.util.trFmt("task.deadline_summary_fmt", "dur" to deadlineLabelText, "date" to deadlineFormatted),
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (timingMode == "NOW") Color(0xFFEF4444) else OozoxTeal
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 5. Optional Financial Amount
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "٤. البند المالي أو الحافز المربوط بالمهمة (اختياري):",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldPrimary
                                )
                                OutlinedTextField(
                                    value = financialAmountStr,
                                    onValueChange = { financialAmountStr = it.filter { c -> c.isDigit() || c == '.' } },
                                    label = { Text("المبلغ المالي أو المكافأة أو التكلفة", fontSize = 11.5.sp) },
                                    placeholder = { Text("مثال: 1500") },
                                    leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(18.dp)) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = financialNotes,
                                    onValueChange = { financialNotes = it },
                                    label = { Text("تفاصيل البند المالي", fontSize = 11.sp) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                    ) {
                        Text(com.example.util.tr("common.cancel"))
                    }

                    Button(
                        onClick = {
                            val deadlineTimestamp = System.currentTimeMillis() + durationMillis
                            val deadlineDate = Date(deadlineTimestamp)
                            val deadlineFormatted = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(deadlineDate)
                            val finAmount = financialAmountStr.toDoubleOrNull() ?: 0.0

                            val task = ProjectTaskEntity(
                                id = "field_task_${System.currentTimeMillis()}",
                                projectId = projectId,
                                projectName = projectName,
                                title = title.trim(),
                                description = description.trim(),
                                assignedToUserId = null,
                                assignedToName = selectedMembers.joinToString("، "),
                                assignedToPhone = null,
                                priority = if (timingMode == "NOW") TaskPriority.URGENT.name else selectedPriority,
                                status = TaskStatus.PENDING.name,
                                dueDate = deadlineFormatted,
                                costEstimate = finAmount,
                                isSynced = true,
                                workGroupId = workGroupId,
                                mediaAttachmentUri = mediaUri,
                                mediaAttachmentType = selectedMediaType,
                                assignedMembersList = selectedMembers.joinToString(" | "),
                                assignedMemberIds = "",
                                deadlineDurationText = deadlineLabelText,
                                deadlineTimestamp = deadlineTimestamp,
                                urgencyAlertLevel = if (timingMode == "NOW") "CRITICAL" else "NORMAL",
                                financialAmount = finAmount,
                                financialNotes = financialNotes.ifBlank { null },
                                timingMode = timingMode,
                                durationValue = numericDuration
                            )

                            onCreateTask(task)
                            onDismiss()
                        },
                        enabled = title.isNotBlank() && selectedMembers.isNotEmpty(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                        modifier = Modifier
                            .weight(1.8f)
                            .heightIn(min = 48.dp)
                            .testTag("confirm_create_field_task_btn")
                    ) {
                        Text("إسناد المهمة وإخطار المكلفين ✓", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

/**
 * Dialog for Worker / Assignee to respond to the field task with photo/video proof and text summary
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerSubmitProofDialog(
    task: ProjectTaskEntity,
    currentUserName: String = "الموظف المكلف",
    currentUserId: String = "worker_1",
    onDismiss: () -> Unit,
    onSubmitProof: (proofUri: String?, proofType: String, notes: String, userId: String, userName: String) -> Unit
) {
    var responseType by remember { mutableStateOf("PHOTO") }
    var proofUri by remember { mutableStateOf<String?>(null) }
    var notes by remember { mutableStateOf("") }
    val proofPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            proofUri = uri.toString()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 12.dp,
            border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Verified, contentDescription = null, tint = Color(0xFF10B981))
                        Text(
                            text = "توثيق إنجاز المهمة الميدانية 📸",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                    OozoxCircleIconButton(
                        onClick = onDismiss,
                        imageVector = Icons.Default.Close,
                        contentDescription = "إلغاء"
                    )
                }

                // Task Overview
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(text = "المهمة: ${task.title}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = GoldPrimary)
                        if (!task.description.isNullOrBlank()) {
                            Text(text = "المطلوب: ${task.description}", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(text = "المكلفون: ${task.assignedMembersList.ifBlank { task.assignedToName ?: "" }}", fontSize = 11.sp, color = GoldAccent)
                    }
                }

                // Proof Type Selection
                Text(text = "نوع إثبات الإنجاز الميداني:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GoldPrimary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = responseType == "PHOTO",
                        onClick = {
                            responseType = "PHOTO"
                            proofPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        label = { Text("📷 إرفاق صورة الإنجاز", fontSize = 11.5.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GoldPrimary,
                            selectedLabelColor = Color.Black
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = responseType == "VIDEO",
                        onClick = {
                            responseType = "VIDEO"
                            proofPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                            )
                        },
                        label = { Text("🎥 إرفاق فيديو قصير", fontSize = 11.5.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GoldPrimary,
                            selectedLabelColor = Color.Black
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Proof Status
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (proofUri != null) Color(0xFF10B981).copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, if (proofUri != null) Color(0xFF10B981).copy(alpha = 0.3f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(
                            if (proofUri != null) Icons.Default.CheckCircle else Icons.Default.AttachFile,
                            contentDescription = null,
                            tint = if (proofUri != null) Color(0xFF10B981) else GoldPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = when {
                                proofUri != null && responseType == "VIDEO" -> "تم إرفاق فيديو قصير لتوثيق الإنجاز بنجاح"
                                proofUri != null -> "تم إرفاق صورة إثبات إنجاز العمل بنجاح"
                                else -> "اختياري: اختر صورة أو فيديو قصير من المعرض لإثبات الإنجاز"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Description Text
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("نص يمثل ويشرح إنجاز المهمة المسندة", fontSize = 12.sp) },
                    placeholder = { Text("اكتب ملخص ما تم إنجازه، الملاحظات الميدانية، وجاهزية الموقع للفحص...") },
                    minLines = 3,
                    maxLines = 5,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("إلغاء")
                    }

                    Button(
                        onClick = {
                            onSubmitProof(proofUri, responseType, notes.trim(), currentUserId, currentUserName)
                            onDismiss()
                        },
                        enabled = notes.isNotBlank(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981), contentColor = Color.White),
                        modifier = Modifier.weight(1.8f)
                    ) {
                        Text("إرسال الإثبات للمدير للفحص ✓", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                    }
                }
            }
        }
    }
}

/**
 * Dialog for Admin to verify, confirm, archive, and record financials for a completed field task
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminVerifyTaskDialog(
    task: ProjectTaskEntity,
    onDismiss: () -> Unit,
    onConfirmAndArchive: (financialAmount: Double, notes: String?) -> Unit
) {
    var financialAmountStr by remember { mutableStateOf(if (task.costEstimate > 0) task.costEstimate.toInt().toString() else "") }
    var verificationNotes by remember { mutableStateOf("تم الفحص الميداني والمطابقة الهندسية واعتماد الإنجاز والأرشفة بالسجلات.") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 12.dp,
            border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = GoldPrimary)
                        Text(
                            text = "فحص وتأكيد المهمة الميدانية وأرشفتها 📋",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp
                        )
                    }
                    OozoxCircleIconButton(
                        onClick = onDismiss,
                        imageVector = Icons.Default.Close,
                        contentDescription = "إلغاء"
                    )
                }

                // Worker Proof Summary
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(text = "المهمة: ${task.title}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = GoldPrimary)
                        Text(text = "المكلف: ${task.workerResponseByUserName ?: task.assignedMembersList}", fontSize = 11.5.sp, color = GoldAccent)
                        
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(
                                if (task.workerResponseType == "VIDEO") Icons.Default.Videocam else Icons.Default.PhotoCamera,
                                contentDescription = null,
                                tint = if (task.workerResponseType == "VIDEO") Color(0xFFEF4444) else Color(0xFF10B981),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (task.workerResponseType == "VIDEO") "مرفق فيديو توثيق الموقع المنجز ✓" else "مرفق صورة توثيق العمل المنجز ✓",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (!task.workerResponseText.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.background,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "📝 إفادة المنفذ: ${task.workerResponseText}",
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    }
                }

                // Financial Recording
                OutlinedTextField(
                    value = financialAmountStr,
                    onValueChange = { financialAmountStr = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("المبلغ المالي المعتمد للصرف (أجر/مكافأة/مواد)", fontSize = 11.5.sp) },
                    placeholder = { Text("0.0") },
                    leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = verificationNotes,
                    onValueChange = { verificationNotes = it },
                    label = { Text("ملاحظات الفحص والاعتماد بالسجلات", fontSize = 11.5.sp) },
                    minLines = 2,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("إلغاء")
                    }

                    Button(
                        onClick = {
                            val amount = financialAmountStr.toDoubleOrNull() ?: 0.0
                            onConfirmAndArchive(amount, verificationNotes.ifBlank { null })
                            onDismiss()
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                        modifier = Modifier.weight(2f)
                    ) {
                        Text("تأكيد الإنجاز والأرشفة بالسجلات ✓", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
