package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.ContactEntity
import com.example.data.model.GroupMemberEntity
import com.example.ui.theme.GoldPrimary

@Composable
fun GroupMembersAndAddDialog(
    groupTitle: String,
    ownerId: String?,
    members: List<GroupMemberEntity>,
    contacts: List<ContactEntity>,
    isCurrentUserManager: Boolean,
    initialTab: Int = 0, // 0 = إضافة أعضاء, 1 = الأعضاء الحاليون
    onDismiss: () -> Unit,
    onAddMembers: (
        selectedContacts: List<ContactEntity>,
        manualName: String,
        manualPhone: String,
        roleLabel: String,
        isGroupAdmin: Boolean
    ) -> Unit,
    onToggleAdmin: (GroupMemberEntity) -> Unit = {},
    onRemoveMember: (GroupMemberEntity) -> Unit = {}
) {
    var activeTab by remember(initialTab) { mutableIntStateOf(initialTab) }
    var searchQuery by remember { mutableStateOf("") }
    val selectedContactIds = remember { mutableStateListOf<String>() }
    var manualName by remember { mutableStateOf("") }
    var manualPhone by remember { mutableStateOf("") }
    var roleLabel by remember { mutableStateOf("عضو فريق العمل") }
    var grantAdmin by remember { mutableStateOf(false) }

    val quickRoles = listOf(
        "عضو فريق العمل",
        "مهندس موقع",
        "محاسب",
        "مسؤول مبيعات",
        "مشرف تنفيذ"
    )

    val availableContacts = remember(contacts, members, searchQuery) {
        val q = searchQuery.trim()
        contacts.filter { contact ->
            val alreadyInGroup = members.any { m ->
                m.userId == contact.id ||
                    (contact.phone.isNotBlank() && m.phone == contact.phone) ||
                    m.username.trim().equals(contact.name.trim(), ignoreCase = true)
            }
            val matchesQuery = q.isBlank() ||
                contact.name.contains(q, ignoreCase = true) ||
                contact.phone.contains(q, ignoreCase = true)
            !alreadyInGroup && matchesQuery
        }
    }

    val pendingAddCount = selectedContactIds.size + if (manualName.isNotBlank()) 1 else 0

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shadowElevation = 12.dp,
                border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.35f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp)
                    .heightIn(max = 660.dp)
                    .testTag("group_members_dialog")
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Fixed Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(GoldPrimary.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (activeTab == 0) Icons.Default.PersonAdd else Icons.Default.Groups,
                                    contentDescription = null,
                                    tint = GoldPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = groupTitle.ifBlank { "أعضاء المجموعة" },
                                    fontSize = 15.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${members.size} عضو حالي",
                                    fontSize = 11.5.sp,
                                    color = GoldPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("group_members_close_btn")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "إغلاق")
                        }
                    }

                    // Fixed Mode Switcher (Add vs Current Members)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = activeTab == 0,
                            onClick = { activeTab = 0 },
                            leadingIcon = {
                                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            label = {
                                Text("إضافة أعضاء", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GoldPrimary.copy(alpha = 0.18f),
                                selectedLabelColor = GoldPrimary,
                                selectedLeadingIconColor = GoldPrimary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("group_members_tab_add")
                        )
                        FilterChip(
                            selected = activeTab == 1,
                            onClick = { activeTab = 1 },
                            leadingIcon = {
                                Icon(Icons.Default.Groups, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            label = {
                                Text("الأعضاء (${members.size})", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GoldPrimary.copy(alpha = 0.18f),
                                selectedLabelColor = GoldPrimary,
                                selectedLeadingIconColor = GoldPrimary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("group_members_tab_list")
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                    // Scrollable Body
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .testTag("group_members_scroll_body"),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (activeTab == 0) {
                            // Contact search + checklist
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                label = { Text("بحث في جهات الاتصال...", fontSize = 12.sp) },
                                leadingIcon = {
                                    Icon(Icons.Default.Search, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(18.dp))
                                },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(Icons.Default.Close, contentDescription = "مسح", modifier = Modifier.size(16.dp))
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("group_add_member_search")
                            )

                            Text(
                                text = "اختر من جهات الاتصال (${selectedContactIds.size} محدد):",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldPrimary
                            )

                            if (availableContacts.isEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = if (contacts.isEmpty()) {
                                            "لا توجد جهات اتصال محفوظة — أضف العضو بالاسم والهاتف بالأسفل"
                                        } else if (searchQuery.isNotBlank()) {
                                            "لا توجد نتائج مطابقة للبحث"
                                        } else {
                                            "جميع جهات الاتصال مضافون بالفعل — يمكنك إضافة عضو جديد بالأسفل"
                                        },
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    availableContacts.forEach { contact ->
                                        val isSelected = selectedContactIds.contains(contact.id)
                                        Surface(
                                            onClick = {
                                                if (isSelected) selectedContactIds.remove(contact.id)
                                                else selectedContactIds.add(contact.id)
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isSelected) GoldPrimary.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                                            border = BorderStroke(
                                                width = if (isSelected) 1.dp else 0.6.dp,
                                                color = if (isSelected) GoldPrimary.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(min = 48.dp)
                                                .testTag("group_add_contact_item_${contact.id}")
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Checkbox(
                                                    checked = isSelected,
                                                    onCheckedChange = null,
                                                    colors = CheckboxDefaults.colors(checkedColor = GoldPrimary),
                                                    modifier = Modifier.size(22.dp)
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = contact.name,
                                                        fontSize = 13.5.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    if (contact.phone.isNotBlank()) {
                                                        Text(
                                                            text = contact.phone,
                                                            fontSize = 11.5.sp,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                            Text(
                                text = "أو إضافة عضو جديد مباشرة:",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldPrimary
                            )

                            OutlinedTextField(
                                value = manualName,
                                onValueChange = { manualName = it },
                                label = { Text("اسم العضو الجديد", fontSize = 12.sp) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("group_add_manual_name_input")
                            )

                            OutlinedTextField(
                                value = manualPhone,
                                onValueChange = { manualPhone = it },
                                label = { Text("رقم الهاتف (اختياري)", fontSize = 12.sp) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("group_add_manual_phone_input")
                            )

                            Text(
                                text = "الصفة / الدور الوظيفي:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                quickRoles.forEach { preset ->
                                    val chosen = roleLabel == preset
                                    FilterChip(
                                        selected = chosen,
                                        onClick = { roleLabel = preset },
                                        label = { Text(preset, fontSize = 11.5.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = GoldPrimary.copy(alpha = 0.18f),
                                            selectedLabelColor = GoldPrimary
                                        )
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = roleLabel,
                                onValueChange = { roleLabel = it },
                                label = { Text("المسمى الوظيفي المخصص", fontSize = 12.sp) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("group_add_role_input")
                            )

                            Surface(
                                onClick = { grantAdmin = !grantAdmin },
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("group_add_admin_switch_row")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(20.dp))
                                        Text("منح صلاحية مدير المجموعة", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                    Switch(
                                        checked = grantAdmin,
                                        onCheckedChange = { grantAdmin = it },
                                        modifier = Modifier.testTag("group_add_admin_switch")
                                    )
                                }
                            }
                        } else {
                            // Current Members List
                            if (members.isEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(18.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.GroupAdd, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(32.dp))
                                        Text("لا يوجد أعضاء مسجلون بعد", fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                                        TextButton(onClick = { activeTab = 0 }) {
                                            Text("إضافة أعضاء الآن", color = GoldPrimary, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    members.forEach { member ->
                                        val isOwner = !ownerId.isNullOrBlank() && member.userId == ownerId
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f),
                                            border = BorderStroke(
                                                0.8.dp,
                                                if (member.isGroupAdmin) GoldPrimary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("group_member_row_${member.id}")
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(40.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                            if (member.isGroupAdmin) GoldPrimary.copy(alpha = 0.2f)
                                                            else Color(0xFF3B82F6).copy(alpha = 0.16f)
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = member.username.trim().take(1).ifBlank { "ع" },
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (member.isGroupAdmin) GoldPrimary else Color(0xFF3B82F6),
                                                        fontSize = 15.sp
                                                    )
                                                }
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Text(
                                                            text = "@${member.username.removePrefix("@")}",
                                                            fontSize = 13.5.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis,
                                                            modifier = Modifier.weight(1f, fill = false)
                                                        )
                                                        if (member.isGroupAdmin) {
                                                            Surface(
                                                                shape = RoundedCornerShape(6.dp),
                                                                color = GoldPrimary.copy(alpha = 0.16f)
                                                            ) {
                                                                Text(
                                                                    text = if (isOwner) "المالك" else "مدير",
                                                                    fontSize = 10.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = GoldPrimary,
                                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                    Text(
                                                        text = listOfNotNull(
                                                            member.roleLabel.takeIf { it.isNotBlank() },
                                                            member.phone.takeIf { it.isNotBlank() }
                                                        ).joinToString(" • "),
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }

                                                if (isCurrentUserManager && !isOwner) {
                                                    IconButton(
                                                        onClick = { onToggleAdmin(member) },
                                                        modifier = Modifier
                                                            .size(48.dp)
                                                            .testTag("toggle_admin_member_${member.id}")
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.AdminPanelSettings,
                                                            contentDescription = if (member.isGroupAdmin) "إلغاء صلاحية المدير" else "ترقية لمدير",
                                                            tint = if (member.isGroupAdmin) GoldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                    IconButton(
                                                        onClick = { onRemoveMember(member) },
                                                        modifier = Modifier
                                                            .size(48.dp)
                                                            .testTag("remove_member_${member.id}")
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.PersonRemove,
                                                            contentDescription = "إزالة العضو",
                                                            tint = MaterialTheme.colorScheme.error
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                    // Fixed Footer Actions
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
                                .heightIn(min = 48.dp)
                                .testTag("group_members_cancel_btn")
                        ) {
                            Text("إغلاق", fontSize = 13.5.sp)
                        }
                        if (activeTab == 0) {
                            Button(
                                onClick = {
                                    val chosenContacts = contacts.filter { it.id in selectedContactIds }
                                    onAddMembers(
                                        chosenContacts,
                                        manualName.trim(),
                                        manualPhone.trim(),
                                        roleLabel.trim().ifBlank { "عضو فريق العمل" },
                                        grantAdmin
                                    )
                                },
                                enabled = pendingAddCount > 0,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = GoldPrimary,
                                    contentColor = Color.Black
                                ),
                                modifier = Modifier
                                    .weight(1.4f)
                                    .heightIn(min = 48.dp)
                                    .testTag("group_members_confirm_add_btn")
                            ) {
                                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (pendingAddCount > 0) "إضافة للمجموعة ($pendingAddCount)" else "إضافة للمجموعة",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Button(
                                onClick = { activeTab = 0 },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = GoldPrimary,
                                    contentColor = Color.Black
                                ),
                                modifier = Modifier
                                    .weight(1.4f)
                                    .heightIn(min = 48.dp)
                                    .testTag("group_members_switch_to_add_btn")
                            ) {
                                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("إضافة عضو", fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
