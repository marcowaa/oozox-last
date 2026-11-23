package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.example.data.model.GroupMemberEntity
import com.example.data.model.HiddenSubAccountEntity
import com.example.data.model.ProjectMemberEntity
import com.example.data.model.ProjectMemberRole
import com.example.ui.theme.*
import com.example.util.LocalAppLanguage
import com.example.util.tr

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProjectRbacDialog(
    projectId: String,
    members: List<ProjectMemberEntity>,
    groupMembers: List<GroupMemberEntity> = emptyList(),
    employees: List<HiddenSubAccountEntity> = emptyList(),
    onAddMember: (name: String, phone: String, role: ProjectMemberRole, task: String) -> Unit,
    onAddMemberWithPermissions: ((name: String, phone: String, role: ProjectMemberRole, task: String, userId: String?, isAdmin: Boolean, canPost: Boolean, canEdit: Boolean, canExport: Boolean, canDirect: Boolean) -> Unit)? = null,
    onPromoteToManager: (ProjectMemberEntity) -> Unit = {},
    onRemoveMember: ((ProjectMemberEntity) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val currentLanguage = LocalAppLanguage.current
    var showAddForm by remember { mutableStateOf(false) }
    var userIdInput by remember { mutableStateOf<String?>(null) }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(ProjectMemberRole.EXECUTIVE_MANAGER) }
    var task by remember { mutableStateOf("") }
    var permAdmin by remember { mutableStateOf(ProjectMemberRole.EXECUTIVE_MANAGER.defaultIsAdmin()) }
    var permPost by remember { mutableStateOf(ProjectMemberRole.EXECUTIVE_MANAGER.defaultCanPostAccounting()) }
    var permEdit by remember { mutableStateOf(ProjectMemberRole.EXECUTIVE_MANAGER.defaultCanEditRecords()) }
    var permExport by remember { mutableStateOf(ProjectMemberRole.EXECUTIVE_MANAGER.defaultCanExportReports()) }
    var permDirect by remember { mutableStateOf(true) }
    var showEmployeeList by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 12.dp,
            border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(GoldPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = null,
                                tint = GoldPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "فريق العمل والصلاحيات",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "تحديد مسؤوليات الأعضاء والشركاء",
                                fontSize = 11.sp,
                                color = GoldPrimary
                            )
                        }
                    }

                    OozoxCircleIconButton(
                        onClick = onDismiss,
                        imageVector = Icons.Default.Close,
                        contentDescription = "إغلاق",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                    thickness = 0.8.dp
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (!showAddForm) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("أعضاء فريق المشروع (${members.size}):", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Button(
                                onClick = { showAddForm = true },
                                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = MaterialTheme.colorScheme.background),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("إضافة عضو", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (members.isEmpty()) {
                            Text(tr("proj_group.no_members_yet", "لا يوجد أعضاء مضافون في الهيكل الإداري للمشروع."), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.heightIn(max = 280.dp)) {
                                items(members, key = { it.id }) { member ->
                                    val roleEnum = ProjectMemberRole.entries.find { it.name == member.role } ?: ProjectMemberRole.EXECUTIVE_MANAGER
                                    val canPromote = roleEnum != ProjectMemberRole.OWNER && roleEnum != ProjectMemberRole.EXECUTIVE_MANAGER
                                    val matchedGm = groupMembers.firstOrNull { gm ->
                                        gm.userId == member.userId ||
                                            (gm.phone.isNotBlank() && gm.phone == member.userPhone) ||
                                            gm.username.equals(member.userName, ignoreCase = true)
                                    }
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(member.userName, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                                                    val subInfo = listOfNotNull(
                                                        member.userPhone.takeIf { it.isNotBlank() },
                                                        member.assignedTask?.takeIf { it.isNotBlank() }
                                                    ).joinToString(" • ")
                                                    if (subInfo.isNotBlank()) {
                                                        Text(subInfo, fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    }
                                                }

                                                Surface(
                                                    color = GoldPrimary.copy(alpha = 0.15f),
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text(roleEnum.localizedLabel(currentLanguage), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = GoldPrimary, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                                }
                                            }
                                            if (matchedGm != null) {
                                                val perms = buildList {
                                                    if (matchedGm.isGroupAdmin) add(tr("proj_group.perm_admin", "مدير مجموعة"))
                                                    if (matchedGm.canPostAccounting) add(tr("proj_group.perm_accounting", "قيود مالية"))
                                                    if (matchedGm.canEditRecords) add(tr("proj_group.perm_edit", "تعديل"))
                                                    if (matchedGm.canExportReports) add(tr("proj_group.perm_export", "تقارير"))
                                                }
                                                if (perms.isNotEmpty()) {
                                                    Text(perms.joinToString(" • "), fontSize = 10.sp, color = GoldPrimary, fontWeight = FontWeight.Medium)
                                                }
                                            }
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.End,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                if (canPromote) {
                                                    FilledTonalButton(
                                                        onClick = { onPromoteToManager(member) },
                                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                        shape = RoundedCornerShape(8.dp),
                                                        colors = ButtonDefaults.filledTonalButtonColors(
                                                            containerColor = Color(0xFF10B981).copy(alpha = 0.14f),
                                                            contentColor = Color(0xFF10B981)
                                                        )
                                                    ) {
                                                        Icon(Icons.Default.Upgrade, contentDescription = null, modifier = Modifier.size(15.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("ترقية بصلاحيات مدير", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                                if (onRemoveMember != null && roleEnum != ProjectMemberRole.OWNER) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    OozoxCircleIconButton(
                                                        onClick = { onRemoveMember(member) },
                                                        imageVector = Icons.Default.DeleteOutline,
                                                        contentDescription = "إزالة",
                                                        tint = MaterialTheme.colorScheme.error,
                                                        containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .heightIn(max = 380.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("إضافة عضو وتحديد وظيفته وصلاحياته:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GoldPrimary)
                                if (employees.any { it.isActiveEmployee }) {
                                    TextButton(onClick = { showEmployeeList = !showEmployeeList }) {
                                        Icon(Icons.Default.Badge, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(tr("proj_group.pick_from_employees", "من موظفي الشركة"), fontSize = 11.sp, color = GoldPrimary)
                                    }
                                }
                            }

                            if (showEmployeeList) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    employees.filter { it.isActiveEmployee }.forEach { emp ->
                                        AssistChip(
                                            onClick = {
                                                userIdInput = emp.id
                                                name = emp.displayName
                                                phone = emp.phone.orEmpty()
                                                task = emp.roleTitle.ifBlank { emp.department }
                                                val autoRole = when {
                                                    emp.permManageProjects || emp.permManageBusiness || emp.permManageStaff -> ProjectMemberRole.EXECUTIVE_MANAGER
                                                    emp.canViewReports -> ProjectMemberRole.FINANCIAL_PARTNER
                                                    else -> ProjectMemberRole.PROCUREMENT_OFFICER
                                                }
                                                selectedRole = autoRole
                                                permAdmin = emp.permManageBusiness || emp.permManageStaff || emp.permManageProjects
                                                permPost = emp.permManageAccounting || emp.canCreateInvoices
                                                permEdit = emp.permManageProjects || emp.permAssignTasks
                                                permExport = emp.canViewReports
                                                permDirect = emp.permDirectChat
                                                showEmployeeList = false
                                            },
                                            label = { Text("${emp.displayName} (${emp.roleTitle})", fontSize = 11.sp) }
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = { Text("اسم الموظف / الشريك") },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = phone,
                                onValueChange = { phone = it },
                                label = { Text("رقم الهاتف") },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = task,
                                onValueChange = { task = it },
                                label = { Text(tr("proj_group.job_title_label", "الوظيفة المسندة في المشروع")) },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Text(tr("proj_group.project_role_label", "حدد الدور الإداري في المشروع:"), fontSize = 11.sp, color = GoldPrimary, fontWeight = FontWeight.Bold)

                            ProjectMemberRole.entries.filter { it != ProjectMemberRole.OWNER }.forEach { role ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    RadioButton(
                                        selected = selectedRole == role,
                                        onClick = {
                                            selectedRole = role
                                            permAdmin = role.defaultIsAdmin()
                                            permPost = role.defaultCanPostAccounting()
                                            permEdit = role.defaultCanEditRecords()
                                            permExport = role.defaultCanExportReports()
                                            permDirect = true
                                        }
                                    )
                                    Column {
                                        Text(role.localizedLabel(currentLanguage), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Text(role.localizedDescription(currentLanguage), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }

                            Text(tr("proj_group.permissions_title", "صلاحيات الموظف داخل المشروع والمجموعة:"), fontSize = 11.sp, color = GoldPrimary, fontWeight = FontWeight.Bold)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                listOf(
                                    Triple(tr("proj_group.perm_admin", "مدير مجموعة"), permAdmin) { v: Boolean -> permAdmin = v },
                                    Triple(tr("proj_group.perm_accounting", "قيود مالية"), permPost) { v: Boolean -> permPost = v },
                                    Triple(tr("proj_group.perm_edit", "تعديل السجلات"), permEdit) { v: Boolean -> permEdit = v },
                                    Triple(tr("proj_group.perm_export", "تصدير تقارير"), permExport) { v: Boolean -> permExport = v },
                                    Triple(tr("proj_group.perm_direct_chat", "تراسل مباشر"), permDirect) { v: Boolean -> permDirect = v }
                                ).forEach { (permLabel, checked, onToggle) ->
                                    FilterChip(
                                        selected = checked,
                                        onClick = { onToggle(!checked) },
                                        leadingIcon = if (checked) {
                                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(13.dp)) }
                                        } else null,
                                        label = { Text(permLabel, fontSize = 10.5.sp) },
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                    thickness = 0.8.dp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            if (showAddForm) showAddForm = false else onDismiss()
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                    ) {
                        Text(if (showAddForm) "رجوع" else "إغلاق", fontSize = 13.sp)
                    }

                    if (showAddForm) {
                        Button(
                            onClick = {
                                if (name.isNotBlank()) {
                                    if (onAddMemberWithPermissions != null) {
                                        onAddMemberWithPermissions(
                                            name.trim(),
                                            phone.trim(),
                                            selectedRole,
                                            task.trim(),
                                            userIdInput,
                                            permAdmin,
                                            permPost,
                                            permEdit,
                                            permExport,
                                            permDirect
                                        )
                                    } else {
                                        onAddMember(name.trim(), phone.trim(), selectedRole, task.trim())
                                    }
                                    showAddForm = false
                                    userIdInput = null
                                    name = ""
                                    phone = ""
                                    task = ""
                                }
                            },
                            enabled = name.isNotBlank(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.White),
                            modifier = Modifier
                                .weight(1.8f)
                                .heightIn(min = 48.dp)
                        ) {
                            Text("حفظ وسند الصلاحية", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                        }
                    }
                }
            }
        }
    }
}
