package com.example.ui.spatial

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.ChatEntity
import com.example.data.model.GoldPricesModel
import com.example.ui.theme.*
import com.example.util.GoldUtils

data class CommandActionItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val category: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommandCenterDialog(
    chats: List<ChatEntity>,
    goldPrices: GoldPricesModel,
    onDismiss: () -> Unit,
    onSelectChat: (String) -> Unit,
    onNewChatClick: () -> Unit,
    onGoldDealClick: () -> Unit,
    onNewProjectClick: () -> Unit,
    onSmartAccountantClick: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val themeController = com.example.ui.LocalAppTheme.current

    val quickCommands = remember(query, chats, themeController.isDarkTheme) {
        val list = mutableListOf<CommandActionItem>()

        list.add(
            CommandActionItem(
                id = "cmd_toggle_theme",
                title = if (themeController.isDarkTheme) "التبديل إلى الوضع الفاتح ☀️" else "التبديل إلى الوضع الداكن 🌙",
                subtitle = "تغيير مظهر التطبيق بالكامل وحفظه في الإعدادات",
                category = "المظهر والنظام",
                icon = if (themeController.isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                onClick = { themeController.toggleTheme(); onDismiss() }
            )
        )

        // High priority quick actions
        list.add(
            CommandActionItem(
                id = "cmd_smart_ai",
                title = "افتح المحاسب المالي",
                subtitle = "استفسارات مالية وتحليل صفقات تلقائي",
                category = "التحليل المالي",
                icon = Icons.Default.AutoAwesome,
                onClick = { onSmartAccountantClick(); onDismiss() }
            )
        )

        list.add(
            CommandActionItem(
                id = "cmd_gold_deal",
                title = "تسجيل صفقة ذهب جديدة",
                subtitle = "أسعار اليوم: عيار 21 = ${GoldUtils.formatCurrency(goldPrices.karat21)}",
                category = "صفقات الذهب",
                icon = Icons.Default.Diamond,
                onClick = { onGoldDealClick(); onDismiss() }
            )
        )

        list.add(
            CommandActionItem(
                id = "cmd_new_project",
                title = "إنشاء مشروع / ميزانية جديدة",
                subtitle = "متابعة رأس المال والمجموعات التجارية",
                category = "المشاريع",
                icon = Icons.Default.BusinessCenter,
                onClick = { onNewProjectClick(); onDismiss() }
            )
        )

        list.add(
            CommandActionItem(
                id = "cmd_new_chat",
                title = "محادثة جديدة",
                subtitle = "بدء محادثة مع جهة اتصال",
                category = "المحادثات",
                icon = Icons.Default.AddComment,
                onClick = { onNewChatClick(); onDismiss() }
            )
        )

        // Matching chats
        chats.forEach { chat ->
            list.add(
                CommandActionItem(
                    id = "chat_${chat.id}",
                    title = "محادثة: ${chat.contactName}",
                    subtitle = chat.lastMessage ?: "لا توجد رسائل",
                    category = "المحادثات القائمة",
                    icon = Icons.Default.ChatBubble,
                    onClick = { onSelectChat(chat.id); onDismiss() }
                )
            )
        }

        if (query.isBlank()) list
        else list.filter {
            it.title.contains(query, ignoreCase = true) ||
                    it.subtitle.contains(query, ignoreCase = true) ||
                    it.category.contains(query, ignoreCase = true)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Command Center Title Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(GoldPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Terminal,
                                contentDescription = null,
                                tint = GoldPrimary
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "مركز الأوامر المتقدم Command Center",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "ابحث عن الأشخاص، الصفقات، أو الأوامر المباشرة",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Search Input Field
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("اكتب أمراً أو اسم جهة اتصال...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = GoldPrimary) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "مسح")
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        focusedBorderColor = GoldPrimary,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Action List
                if (quickCommands.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "لا توجد نتائج تطابق \"$query\"",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(quickCommands, key = { it.id }) { item ->
                            CommandItemCard(item = item)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CommandItemCard(item: CommandActionItem) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    Surface(
        color = if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (isDark) GoldPrimary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { item.onClick() }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isDark) GoldPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = if (isDark) GoldPrimary else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = item.category,
                        fontSize = 10.sp,
                        color = adaptiveAccent
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = item.subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}
