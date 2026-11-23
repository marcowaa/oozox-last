package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.BroadcastTargetCategory
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BroadcastMessagingDialog(
    onDismiss: () -> Unit,
    onSendBroadcast: (
        targetCategory: BroadcastTargetCategory,
        title: String,
        content: String,
        isUrgent: Boolean,
        postAsAnnouncement: Boolean,
        onComplete: (Int) -> Unit
    ) -> Unit
) {
    var selectedCategory by remember { mutableStateOf(BroadcastTargetCategory.ALL_USERS) }
    var broadcastTitle by remember { mutableStateOf("بيان رسمي من إدارة ZOX المركزية") }
    var broadcastContent by remember { mutableStateOf("") }
    var isUrgent by remember { mutableStateOf(false) }
    var postAsAnnouncement by remember { mutableStateOf(true) }

    var isSending by remember { mutableStateOf(false) }
    var sentSuccessCount by remember { mutableStateOf<Int?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val quickTitleTemplates = listOf(
        "تحديث هام لأسعار الذهب والتسويات المالية",
        "بيان أمني رسمي من إدارة ZOX ️",
        "إشعار تحديث المنصة والميزات المحاسبية",
        "تنبيه هام لكافة التجار والمستثمرين"
    )

    Dialog(
        onDismissRequest = {
            if (!isSending) onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(
                1.5.dp,
                Brush.verticalGradient(listOf(GoldPrimary, GoldDark))
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OozoxCircleIconButton(
                        onClick = { if (!isSending) onDismiss() },
                        imageVector = Icons.Default.Close,
                        contentDescription = "إغلاق",
                        tint = MaterialTheme.colorScheme.onSurface,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "البث الجماعي",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldPrimary
                            )
                        }
                        Text(
                            "إرسال تنبيهات جماعية",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(GoldPrimary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Campaign,
                            contentDescription = null,
                            tint = GoldPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Success confirmation view
                if (sentSuccessCount != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF064E3B).copy(alpha = 0.4f)),
                        border = BorderStroke(1.dp, Color(0xFF10B981)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                "تم الإرسال بنجاح",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                "تم تسليم الرسالة إلى $sentSuccessCount مستخدم بنجاح.",
                                fontSize = 13.sp,
                                color = Color(0xFFD1FAE5),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = onDismiss,
                                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary)
                            ) {
                                Text("تم", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    // Category Selection
                    Text(
                        "الفئة المستهدفة:",
                        modifier = Modifier.fillMaxWidth(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BroadcastTargetCategory.values().forEach { cat ->
                            val isSelected = selectedCategory == cat
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { selectedCategory = cat },
                                color = if (isSelected) GoldPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                border = BorderStroke(
                                    if (isSelected) 1.5.dp else 1.dp,
                                    if (isSelected) GoldPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(cat.icon, fontSize = 20.sp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            cat.label,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 13.sp,
                                            color = if (isSelected) GoldPrimary else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            cat.description,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { selectedCategory = cat },
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = GoldPrimary,
                                            unselectedColor = MaterialTheme.colorScheme.outline
                                        )
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Title & Quick Templates
                    Text(
                        "عنوان الرسالة:",
                        modifier = Modifier.fillMaxWidth(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = broadcastTitle,
                        onValueChange = { broadcastTitle = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("أدخل عنوان الرسالة...") },
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.Title, contentDescription = null, tint = GoldPrimary)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldPrimary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "نماذج مقترحة:",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        quickTitleTemplates.take(2).forEach { tmpl ->
                            SuggestionChip(
                                onClick = { broadcastTitle = tmpl },
                                label = { Text(tmpl.take(22) + "...", fontSize = 10.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Message Content
                    Text(
                        "نص الرسالة:",
                        modifier = Modifier.fillMaxWidth(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = broadcastContent,
                        onValueChange = { broadcastContent = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        placeholder = {
                            Text("اكتب نص الرسالة هنا...")
                        },
                        leadingIcon = {
                            Column(modifier = Modifier.fillMaxHeight(), verticalArrangement = Arrangement.Top) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Icon(Icons.Default.EditNote, contentDescription = null, tint = GoldPrimary)
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldPrimary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Options: Urgency & System Banner
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = if (isUrgent) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("رسالة عاجلة", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                }
                                Switch(
                                    checked = isUrgent,
                                    onCheckedChange = { isUrgent = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFEF4444))
                                )
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Campaign,
                                        contentDescription = null,
                                        tint = GoldPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("نشر في شريط الإعلانات", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                }
                                Switch(
                                    checked = postAsAnnouncement,
                                    onCheckedChange = { postAsAnnouncement = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = GoldPrimary)
                                )
                            }
                        }
                    }

                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            errorMessage ?: "",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isSending
                        ) {
                            Text("إلغاء")
                        }

                        Button(
                            onClick = {
                                if (broadcastTitle.isBlank()) {
                                    errorMessage = "يرجى كتابة عنوان الرسالة"
                                    return@Button
                                }
                                if (broadcastContent.isBlank()) {
                                    errorMessage = "يرجى كتابة نص الرسالة الجماعية"
                                    return@Button
                                }
                                errorMessage = null
                                isSending = true
                                onSendBroadcast(
                                    selectedCategory,
                                    broadcastTitle.trim(),
                                    broadcastContent.trim(),
                                    isUrgent,
                                    postAsAnnouncement
                                ) { count ->
                                    isSending = false
                                    sentSuccessCount = count
                                }
                            },
                            modifier = Modifier.weight(2f),
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isSending
                        ) {
                            if (isSending) {
                                CircularProgressIndicator(
                                    color = Color.Black,
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("جاري الإرسال...", color = Color.Black, fontSize = 13.sp)
                            } else {
                                Icon(Icons.Default.Send, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("إرسال", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
