package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.AssignmentInd
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PhoneDisabled
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import com.example.ui.components.OozoxCircleButtonDefaults
import com.example.ui.components.OozoxCircleIconButton
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.ChatEntity
import com.example.data.model.GroupMemberEntity
import com.example.data.model.HashtagAccountEntity
import com.example.data.model.InvoiceEntity
import com.example.data.model.InvoiceStatus
import com.example.data.model.MessageEntity
import com.example.data.model.MessageType
import com.example.data.model.ProjectEntity
import com.example.data.model.UserEntity
import com.example.data.model.UserType
import com.example.data.model.WorkGroupEntity
import com.example.ui.components.CallType
import com.example.ui.components.MediaPreviewItem
import com.example.ui.components.OozoxAvatarImage
import com.example.ui.components.OozoxThumbnailImage
import com.example.ui.spatial.ChatSpatialMode
import com.example.ui.spatial.SpatialElevation
import com.example.ui.spatial.VoiceCaptureSpace
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusOrange
import com.example.ui.theme.StatusRed
import com.example.ui.theme.adaptiveGoldPrimary
import com.example.ui.theme.adaptiveOnPrimary
import com.example.util.AudioPlayerManager
import com.example.util.AudioRecorderManager
import com.example.util.CallPrivacyManager
import com.example.util.DocumentInfo
import com.example.util.FileUtils
import com.example.util.GoldUtils
import com.example.util.HashtagUtils
import com.example.util.SpeechToTextManager
import java.io.File
import java.util.Locale
import kotlin.math.roundToInt

@Composable
internal fun ChatDetailTopBar(
    chat: ChatEntity,
    contactUser: UserEntity?,
    isZoxAdminChat: Boolean,
    activeGroupMembers: List<GroupMemberEntity>,
    isCurrentUserGroupManager: Boolean,
    parentCompanyGroup: WorkGroupEntity?,
    subDepartmentGroups: List<WorkGroupEntity>,
    invoicesForChat: List<InvoiceEntity>,
    hashtags: List<HashtagAccountEntity>,
    chatStreamFilter: String,
    showChatSearchBar: Boolean,
    chatSearchQuery: String,
    activeSpatialMode: ChatSpatialMode,
    isTopModesRailExpanded: Boolean,
    linkedProject: ProjectEntity? = null,
    linkedProjectTasksCount: Int = 0,
    onBackClick: () -> Unit,
    onShowMessengerOptions: () -> Unit,
    onCallAttempt: (CallType) -> Unit,
    onCallPrivacyBlocked: (String) -> Unit,
    onZoxCallBlocked: () -> Unit,
    onStreamFilterChange: (String) -> Unit,
    onOpenQuickHashtag: () -> Unit,
    onToggleSearchBar: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onOpenLedgerSheet: () -> Unit,
    onOpenGroupMembersDialog: (initialTab: Int) -> Unit,
    onOpenCreateTaskDialog: () -> Unit,
    onOpenGroupChatById: (String) -> Unit,
    onSelectSpatialMode: (ChatSpatialMode) -> Unit,
    onOpenProjectWorkspace: () -> Unit = {},
    onOpenQuickProjectAccounting: () -> Unit = {},
    onOpenFullProjectDetail: () -> Unit = {}
) {
    val context = LocalContext.current

    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        tonalElevation = 1.dp,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(50.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "رجوع",
                        tint = GoldPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onShowMessengerOptions() }
                        .padding(horizontal = 4.dp, vertical = 3.dp)
                ) {
                    Box(modifier = Modifier.size(36.dp)) {
                        OozoxAvatarImage(
                            model = contactUser?.avatarUri,
                            name = chat.contactName,
                            size = 36.dp,
                            isGroup = chat.isGroup,
                            isTrader = chat.contactType == UserType.GOLD_TRADER.name,
                            contentDescription = chat.contactName
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981))
                                .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = chat.contactName,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (chat.contactVerified) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = "موثق",
                                    tint = GoldAccent,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        Text(
                            text = when {
                                isZoxAdminChat -> "الإدارة الرسمية"
                                chat.isGroup && activeGroupMembers.isNotEmpty() -> "مجموعة نشطة • ${activeGroupMembers.size} أعضاء"
                                chat.isGroup -> "مجموعة نشطة"
                                chat.contactType == UserType.GOLD_TRADER.name -> "نشط الآن • تاجر معتمد"
                                else -> "نشط الآن"
                            },
                            fontSize = 10.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = if (isZoxAdminChat) Color(0xFF10B981) else GoldAccent
                        )
                    }
                }

                IconButton(
                    onClick = {
                        val (allowed, reason) = CallPrivacyManager.checkCallAllowed(
                            context = context,
                            isContactSaved = !chat.isGroup,
                            callerName = chat.contactName
                        )
                        if (!allowed && reason != null) {
                            onCallPrivacyBlocked(reason)
                        } else if (isZoxAdminChat) {
                            onZoxCallBlocked()
                        } else {
                            onCallAttempt(CallType.AUDIO)
                        }
                    },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = if (isZoxAdminChat) Icons.Default.PhoneDisabled else Icons.Default.Call,
                        contentDescription = "اتصال صوتي",
                        tint = if (isZoxAdminChat) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f) else GoldPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = {
                        val (allowed, reason) = CallPrivacyManager.checkCallAllowed(
                            context = context,
                            isContactSaved = !chat.isGroup,
                            callerName = chat.contactName
                        )
                        if (!allowed && reason != null) {
                            onCallPrivacyBlocked(reason)
                        } else if (isZoxAdminChat) {
                            onZoxCallBlocked()
                        } else {
                            onCallAttempt(CallType.VIDEO)
                        }
                    },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = if (isZoxAdminChat) Icons.Default.VideocamOff else Icons.Default.Videocam,
                        contentDescription = "مكالمة فيديو",
                        tint = if (isZoxAdminChat) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f) else GoldPrimary,
                        modifier = Modifier.size(21.dp)
                    )
                }

                IconButton(
                    onClick = onShowMessengerOptions,
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("chat_messenger_info_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = "خيارات ومعلومات المحادثة",
                        tint = GoldPrimary,
                        modifier = Modifier.size(21.dp)
                    )
                }
            }
            HorizontalDivider(
                thickness = 0.6.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )

            ChatStreamFilterAndSearchSection(
                invoicesForChat = invoicesForChat,
                hashtags = hashtags,
                chatStreamFilter = chatStreamFilter,
                showChatSearchBar = showChatSearchBar,
                chatSearchQuery = chatSearchQuery,
                onStreamFilterChange = onStreamFilterChange,
                onOpenQuickHashtag = onOpenQuickHashtag,
                onToggleSearchBar = onToggleSearchBar,
                onSearchQueryChange = onSearchQueryChange,
                onOpenLedgerSheet = onOpenLedgerSheet
            )
        }
    }

    if (chat.isGroup) {
        WorkGroupHierarchyStrip(
            activeGroupMembersCount = activeGroupMembers.size,
            isCurrentUserGroupManager = isCurrentUserGroupManager,
            linkedProject = linkedProject,
            linkedProjectTasksCount = linkedProjectTasksCount,
            parentCompanyGroup = parentCompanyGroup,
            subDepartmentGroups = subDepartmentGroups,
            onOpenGroupMembersDialog = onOpenGroupMembersDialog,
            onOpenCreateTaskDialog = onOpenCreateTaskDialog,
            onOpenGroupChatById = onOpenGroupChatById,
            onOpenProjectWorkspace = onOpenProjectWorkspace,
            onOpenQuickProjectAccounting = onOpenQuickProjectAccounting,
            onOpenFullProjectDetail = onOpenFullProjectDetail
        )
    }

    AnimatedVisibility(
        visible = isTopModesRailExpanded,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        SpatialModesRail(
            activeSpatialMode = activeSpatialMode,
            onSelectSpatialMode = onSelectSpatialMode
        )
    }
}

@Composable
private fun ChatStreamFilterAndSearchSection(
    invoicesForChat: List<InvoiceEntity>,
    hashtags: List<HashtagAccountEntity>,
    chatStreamFilter: String,
    showChatSearchBar: Boolean,
    chatSearchQuery: String,
    onStreamFilterChange: (String) -> Unit,
    onOpenQuickHashtag: () -> Unit,
    onToggleSearchBar: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onOpenLedgerSheet: () -> Unit
) {
    val chatTotalBilled = remember(invoicesForChat) { invoicesForChat.sumOf { it.totalAmount } }
    val chatTotalPaid = remember(invoicesForChat) { invoicesForChat.sumOf { it.paidAmount } }
    val chatRemaining = remember(chatTotalBilled, chatTotalPaid) {
        (chatTotalBilled - chatTotalPaid).coerceAtLeast(0.0)
    }
    val chatOpenInvoicesCount = remember(invoicesForChat) {
        invoicesForChat.count { it.status != InvoiceStatus.PAID.name && (it.totalAmount - it.paidAmount) > 0.01 }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 10.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf(
            "ALL" to "الكل",
            "CHAT_ONLY" to "الدردشة فقط",
            "FINANCE_ONLY" to "الفواتير والهاشتاجات #"
        ).forEach { (key, label) ->
            val selected = chatStreamFilter == key
            FilterChip(
                selected = selected,
                onClick = { onStreamFilterChange(key) },
                label = {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = GoldPrimary.copy(alpha = 0.16f),
                    selectedLabelColor = GoldPrimary
                ),
                modifier = Modifier
                    .height(32.dp)
                    .testTag("chat_stream_filter_$key")
            )
        }

        Surface(
            onClick = onOpenQuickHashtag,
            shape = RoundedCornerShape(16.dp),
            color = GoldPrimary.copy(alpha = 0.14f),
            border = BorderStroke(0.8.dp, GoldPrimary.copy(alpha = 0.45f)),
            modifier = Modifier
                .height(32.dp)
                .testTag("chat_top_hashtag_btn")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("#", color = GoldPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                Text("فاتورة / هاشتاج", color = GoldPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        }

        IconButton(
            onClick = onToggleSearchBar,
            modifier = Modifier
                .size(34.dp)
                .testTag("chat_toggle_search_btn")
        ) {
            Icon(
                imageVector = if (showChatSearchBar) Icons.Default.Close else Icons.Default.Search,
                contentDescription = "بحث في المحادثة",
                tint = GoldPrimary,
                modifier = Modifier.size(18.dp)
            )
        }
    }

    AnimatedVisibility(
        visible = showChatSearchBar,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        OutlinedTextField(
            value = chatSearchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("ابحث في الرسائل أو #...", fontSize = 12.sp) },
            singleLine = true,
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(18.dp))
            },
            trailingIcon = {
                if (chatSearchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = "مسح", modifier = Modifier.size(16.dp))
                    }
                }
            },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 4.dp)
                .testTag("chat_search_input")
        )
    }

    if (invoicesForChat.isNotEmpty() || hashtags.isNotEmpty()) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(0.6.dp, GoldPrimary.copy(alpha = 0.25f)),
            onClick = onOpenLedgerSheet,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 4.dp)
                .testTag("chat_client_ledger_strip")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "الفواتير: ${GoldUtils.formatCurrency(chatTotalBilled)}",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Text(
                        text = "•",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "متبقي: ${GoldUtils.formatCurrency(chatRemaining)}",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (chatRemaining > 0.01) StatusOrange else StatusGreen,
                        maxLines = 1
                    )
                }
                if (chatOpenInvoicesCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = StatusOrange.copy(alpha = 0.14f)
                    ) {
                        Text(
                            text = "$chatOpenInvoicesCount مفتوحة",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = StatusOrange,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                } else {
                    Text(
                        text = "الدفتر (${hashtags.size}#)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun WorkGroupHierarchyStrip(
    activeGroupMembersCount: Int,
    isCurrentUserGroupManager: Boolean,
    linkedProject: ProjectEntity? = null,
    linkedProjectTasksCount: Int = 0,
    parentCompanyGroup: WorkGroupEntity?,
    subDepartmentGroups: List<WorkGroupEntity>,
    onOpenGroupMembersDialog: (Int) -> Unit,
    onOpenCreateTaskDialog: () -> Unit,
    onOpenGroupChatById: (String) -> Unit,
    onOpenProjectWorkspace: () -> Unit = {},
    onOpenQuickProjectAccounting: () -> Unit = {},
    onOpenFullProjectDetail: () -> Unit = {}
) {
    Surface(
        color = GoldPrimary.copy(alpha = 0.08f),
        border = BorderStroke(0.6.dp, GoldPrimary.copy(alpha = 0.25f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("workgroup_hierarchy_strip")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 10.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (linkedProject != null) {
                val netBalance = linkedProject.totalIncome - linkedProject.totalExpenses
                Surface(
                    onClick = onOpenProjectWorkspace,
                    shape = RoundedCornerShape(12.dp),
                    color = GoldPrimary,
                    modifier = Modifier.testTag("chat_header_manage_project_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Apartment, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                        Text(
                            text = "إدارة المشروع (${GoldUtils.formatCurrency(netBalance)} • $linkedProjectTasksCount مهام)",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1
                        )
                    }
                }

                Surface(
                    onClick = onOpenQuickProjectAccounting,
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF10B981).copy(alpha = 0.14f),
                    border = BorderStroke(0.8.dp, Color(0xFF10B981).copy(alpha = 0.45f)),
                    modifier = Modifier.testTag("chat_header_project_entry_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(13.dp))
                        Text("قيد مالي", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                    }
                }

                Surface(
                    onClick = onOpenFullProjectDetail,
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(0.8.dp, GoldPrimary.copy(alpha = 0.4f)),
                    modifier = Modifier.testTag("chat_header_open_project_page_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(12.dp))
                        Text("صفحة المشروع", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = GoldPrimary)
                    }
                }
            }

            if (isCurrentUserGroupManager) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = GoldPrimary.copy(alpha = 0.18f),
                    border = BorderStroke(0.8.dp, GoldPrimary.copy(alpha = 0.45f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(13.dp))
                        Text("صلاحية المدير", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = GoldPrimary)
                    }
                }
            }

            Surface(
                onClick = { onOpenGroupMembersDialog(1) },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                border = BorderStroke(0.6.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
                modifier = Modifier.testTag("chat_header_members_count_btn")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.Groups, contentDescription = "أعضاء المجموعة", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(13.dp))
                    Text(
                        text = "$activeGroupMembersCount أعضاء",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Surface(
                onClick = { onOpenGroupMembersDialog(0) },
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF3B82F6).copy(alpha = 0.14f),
                border = BorderStroke(0.8.dp, Color(0xFF3B82F6).copy(alpha = 0.45f)),
                modifier = Modifier.testTag("chat_header_add_member_btn")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, tint = Color(0xFF3B82F6), modifier = Modifier.size(13.dp))
                    Text("إضافة عضو", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3B82F6))
                }
            }

            Surface(
                onClick = onOpenCreateTaskDialog,
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF10B981).copy(alpha = 0.14f),
                border = BorderStroke(0.8.dp, Color(0xFF10B981).copy(alpha = 0.45f)),
                modifier = Modifier.testTag("chat_header_assign_task_btn")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.AssignmentInd, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(13.dp))
                    Text("إسناد مهمة", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                }
            }

            if (parentCompanyGroup != null) {
                Surface(
                    onClick = { onOpenGroupChatById(parentCompanyGroup.id) },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Text(
                        text = "الرئيسية: ${parentCompanyGroup.name}",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            subDepartmentGroups.forEach { dept ->
                Surface(
                    onClick = { onOpenGroupChatById(dept.id) },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(0.8.dp, GoldPrimary.copy(alpha = 0.35f)),
                    modifier = Modifier.testTag("dept_chip_${dept.id}")
                ) {
                    Text(
                        text = "قسم: ${dept.name}",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GoldPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SpatialModesRail(
    activeSpatialMode: ChatSpatialMode,
    onSelectSpatialMode: (ChatSpatialMode) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        tonalElevation = SpatialElevation.Level2,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ChatSpatialMode.entries.forEach { mode ->
                val selected = activeSpatialMode == mode
                FilterChip(
                    selected = selected,
                    onClick = { onSelectSpatialMode(mode) },
                    label = { Text(mode.label, fontSize = 11.5.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = when (mode) {
                            ChatSpatialMode.GOLD -> GoldDark
                            ChatSpatialMode.FINANCIAL -> Color(0xFF1E293B)
                            ChatSpatialMode.PROJECT -> Color(0xFF1E3A5F)
                            ChatSpatialMode.AI_ACCOUNTANT -> Color(0xFF2E1065)
                            ChatSpatialMode.COMMUNICATION -> MaterialTheme.colorScheme.surfaceVariant
                        },
                        selectedLabelColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        }
    }
}

@Composable
internal fun ChatDetailBottomComposer(
    textInput: String,
    onTextInputChange: (String) -> Unit,
    isVoiceCaptureActive: Boolean,
    onCancelVoiceCapture: () -> Unit,
    isInlineDictating: Boolean,
    dictationLanguage: String,
    dictationBaseText: String,
    dictationLiveText: String,
    inlineSpeechManager: SpeechToTextManager,
    inlineSpeechRms: Float,
    onDictationLanguageToggle: (String) -> Unit,
    onDictationLiveTextChange: (String) -> Unit,
    onInlineDictatingChange: (Boolean) -> Unit,
    isHoldRecording: Boolean,
    holdLiveTranscript: String,
    holdAmpLevel: Float,
    attachedDocumentInfo: DocumentInfo?,
    onClearAttachedDocument: () -> Unit,
    isProcessingMedia: Boolean,
    mediaProcessProgress: Float,
    capturedImageUri: Uri?,
    onClearCapturedImage: () -> Unit,
    replyingToMessage: MessageEntity?,
    onClearReplyingTo: () -> Unit,
    mentionCandidates: List<String>,
    isInlineRecordingAudio: Boolean,
    onInlineRecordingAudioChange: (Boolean) -> Unit,
    isInlineRecordCancelZone: Boolean,
    onInlineRecordCancelZoneChange: (Boolean) -> Unit,
    inlineRecordSeconds: Int,
    onInlineRecordSecondsChange: (Int) -> Unit,
    inlineRecordAmp: Float,
    voiceDraftPath: String?,
    onVoiceDraftPathChange: (String?) -> Unit,
    voiceDraftDurationSec: Int,
    onVoiceDraftDurationChange: (Int) -> Unit,
    isDraftAudioPlaying: Boolean,
    onDraftAudioPlayingChange: (Boolean) -> Unit,
    draftAudioProgress: Float,
    onDraftAudioProgressChange: (Float) -> Unit,
    isActionButtonCameraMode: Boolean,
    onToggleActionButtonCameraMode: () -> Unit,
    isImeOpen: Boolean,
    currentUser: UserEntity?,
    onOpenAttachmentSheet: () -> Unit,
    onOpenVideoEditor: (Uri) -> Unit,
    onOpenImageFrameEditor: (Uri) -> Unit,
    onOpenMediaPreview: (MediaPreviewItem) -> Unit,
    onRequestMicPermission: () -> Unit,
    onLaunchFrontVideoNote: () -> Unit,
    onSendMessage: (String, String) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 5.dp)
        ) {
            if (isVoiceCaptureActive) {
                VoiceCaptureSpace(
                    onCancel = onCancelVoiceCapture,
                    onSendVoiceNote = { duration, audioPath, transcript ->
                        val contentPayload = if (audioPath.isNotBlank()) {
                            "$audioPath||$duration||$transcript"
                        } else {
                            "تسجيل صوتي ($duration ثانية) - $transcript"
                        }
                        onSendMessage(contentPayload, MessageType.VOICE.name)
                        onCancelVoiceCapture()
                    },
                    onSendAsText = { transcribedText ->
                        val tag = HashtagUtils.parseHashtag(transcribedText)
                        val msgType = if (tag != null) MessageType.HASHTAG.name else MessageType.TEXT.name
                        onSendMessage(transcribedText.trim(), msgType)
                        onCancelVoiceCapture()
                    },
                    onInsertIntoInput = { transcribedText ->
                        onTextInputChange(transcribedText)
                        onCancelVoiceCapture()
                    }
                )
            } else {
                InlineDictationBanner(
                    visible = isInlineDictating,
                    dictationLanguage = dictationLanguage,
                    dictationBaseText = dictationBaseText,
                    dictationLiveText = dictationLiveText,
                    inlineSpeechManager = inlineSpeechManager,
                    inlineSpeechRms = inlineSpeechRms,
                    onToggleLanguage = onDictationLanguageToggle,
                    onLiveTextChange = onDictationLiveTextChange,
                    onTextInputChange = onTextInputChange,
                    onInlineDictatingChange = onInlineDictatingChange
                )

                HoldRecordingBanner(
                    visible = isHoldRecording,
                    holdLiveTranscript = holdLiveTranscript,
                    holdAmpLevel = holdAmpLevel
                )

                AttachedDocumentBanner(
                    doc = attachedDocumentInfo,
                    onClear = onClearAttachedDocument
                )

                AnimatedVisibility(
                    visible = isProcessingMedia,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(
                                progress = { mediaProcessProgress },
                                modifier = Modifier.size(20.dp),
                                color = adaptiveGoldPrimary,
                                strokeWidth = 2.5.dp
                            )
                            LinearProgressIndicator(
                                progress = { mediaProcessProgress },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = adaptiveGoldPrimary,
                                trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                            )
                        }
                    }
                }

                AttachedMediaBanner(
                    capturedImageUri = capturedImageUri,
                    mediaProcessProgress = mediaProcessProgress,
                    textInput = textInput,
                    currentUser = currentUser,
                    onOpenMediaPreview = onOpenMediaPreview,
                    onOpenVideoEditor = onOpenVideoEditor,
                    onOpenImageFrameEditor = onOpenImageFrameEditor,
                    onClear = onClearCapturedImage
                )

                ReplyPreviewBanner(
                    replyingToMessage = replyingToMessage,
                    onClear = onClearReplyingTo
                )

                MentionSuggestionsStrip(
                    textInput = textInput,
                    mentionCandidates = mentionCandidates,
                    hideForRecording = isInlineRecordingAudio || voiceDraftPath != null,
                    onSelectMention = onTextInputChange
                )

                ComposerInputRow(
                    textInput = textInput,
                    onTextInputChange = onTextInputChange,
                    attachedDocumentInfo = attachedDocumentInfo,
                    capturedImageUri = capturedImageUri,
                    replyingToMessage = replyingToMessage,
                    isInlineRecordingAudio = isInlineRecordingAudio,
                    onInlineRecordingAudioChange = onInlineRecordingAudioChange,
                    isInlineRecordCancelZone = isInlineRecordCancelZone,
                    onInlineRecordCancelZoneChange = onInlineRecordCancelZoneChange,
                    inlineRecordSeconds = inlineRecordSeconds,
                    onInlineRecordSecondsChange = onInlineRecordSecondsChange,
                    inlineRecordAmp = inlineRecordAmp,
                    voiceDraftPath = voiceDraftPath,
                    onVoiceDraftPathChange = onVoiceDraftPathChange,
                    voiceDraftDurationSec = voiceDraftDurationSec,
                    onVoiceDraftDurationChange = onVoiceDraftDurationChange,
                    isDraftAudioPlaying = isDraftAudioPlaying,
                    onDraftAudioPlayingChange = onDraftAudioPlayingChange,
                    draftAudioProgress = draftAudioProgress,
                    onDraftAudioProgressChange = onDraftAudioProgressChange,
                    isActionButtonCameraMode = isActionButtonCameraMode,
                    onToggleActionButtonCameraMode = onToggleActionButtonCameraMode,
                    isImeOpen = isImeOpen,
                    onOpenAttachmentSheet = onOpenAttachmentSheet,
                    onClearAttachedDocument = onClearAttachedDocument,
                    onClearCapturedImage = onClearCapturedImage,
                    onClearReplyingTo = onClearReplyingTo,
                    onRequestMicPermission = onRequestMicPermission,
                    onLaunchFrontVideoNote = onLaunchFrontVideoNote,
                    onSendMessage = onSendMessage
                )
            }
        }
    }
}

@Composable
private fun InlineDictationBanner(
    visible: Boolean,
    dictationLanguage: String,
    dictationBaseText: String,
    dictationLiveText: String,
    inlineSpeechManager: SpeechToTextManager,
    inlineSpeechRms: Float,
    onToggleLanguage: (String) -> Unit,
    onLiveTextChange: (String) -> Unit,
    onTextInputChange: (String) -> Unit,
    onInlineDictatingChange: (Boolean) -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        val pulseTransition = rememberInfiniteTransition(label = "inline_dict_pulse")
        val pulseScale by pulseTransition.animateFloat(
            initialValue = 0.92f,
            targetValue = 1.15f,
            animationSpec = infiniteRepeatable(
                animation = tween(450, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "inline_dict_scale"
        )

        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.5.dp, GoldPrimary),
            shadowElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .scale(pulseScale)
                                .clip(CircleShape)
                                .background(adaptiveGoldPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "جاري الاستماع",
                                tint = adaptiveOnPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "إملاء صوتي فوري (تحويل الصوت لنص) ️",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldPrimary
                            )
                            Text(
                                text = "تحدث لإدراج الكلمات مباشرة في صندوق الرسالة",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = GoldPrimary.copy(alpha = 0.15f),
                            border = BorderStroke(0.8.dp, GoldPrimary.copy(alpha = 0.4f)),
                            modifier = Modifier.clickable {
                                val nextLang = if (dictationLanguage == "ar-EG") "en-US" else "ar-EG"
                                onToggleLanguage(nextLang)
                                inlineSpeechManager.stopListening()
                                inlineSpeechManager.startListening(
                                    languageCode = nextLang,
                                    onPartial = { partial ->
                                        if (partial.isNotBlank()) {
                                            onLiveTextChange(partial)
                                            onTextInputChange(
                                                if (dictationBaseText.isNotBlank()) "${dictationBaseText.trim()} $partial" else partial
                                            )
                                        }
                                    },
                                    onFinal = { finalText ->
                                        if (finalText.isNotBlank()) {
                                            onLiveTextChange("")
                                            onTextInputChange(
                                                if (dictationBaseText.isNotBlank()) "${dictationBaseText.trim()} $finalText" else finalText
                                            )
                                        }
                                        onInlineDictatingChange(false)
                                    }
                                )
                            }
                        ) {
                            Text(
                                text = if (dictationLanguage == "ar-EG") "عربي" else "EN",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Button(
                            onClick = {
                                inlineSpeechManager.stopListening()
                                onInlineDictatingChange(false)
                                onLiveTextChange("")
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = adaptiveGoldPrimary,
                                contentColor = adaptiveOnPrimary
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("تم", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (dictationLiveText.isNotBlank()) dictationLiveText else "تحدث الآن... جاري التقاط صوتك وتحويله لنص عربي/إنجليزي تلقائياً",
                            fontSize = 12.sp,
                            fontWeight = if (dictationLiveText.isNotBlank()) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (dictationLiveText.isNotBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            repeat(5) { i ->
                                val factor = ((i * 3) % 4 + 2) / 5f
                                val barH = (6 + (inlineSpeechRms * 16 * factor)).dp
                                Box(
                                    modifier = Modifier
                                        .width(2.5.dp)
                                        .height(barH)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(GoldPrimary)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HoldRecordingBanner(
    visible: Boolean,
    holdLiveTranscript: String,
    holdAmpLevel: Float
) {
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(),
        exit = shrinkVertically()
    ) {
        val pulseTransition = rememberInfiniteTransition(label = "hold_pulse")
        val pulseScale by pulseTransition.animateFloat(
            initialValue = 0.96f,
            targetValue = 1.05f,
            animationSpec = infiniteRepeatable(
                animation = tween(450, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "hold_scale"
        )

        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.5.dp, GoldPrimary),
            shadowElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .scale(pulseScale)
                                .clip(CircleShape)
                                .background(StatusOrange),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "جاري تحويل الصوت إلى نص مباشرة",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldPrimary
                            )
                            Text(
                                text = "حرر للإرسال الفوري كرسالة نصية",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(8) { i ->
                            val factor = ((i * 7) % 5 + 3) / 8f
                            val barH = (10 + (holdAmpLevel * 20 * factor)).dp
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(barH)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(GoldPrimary)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (holdLiveTranscript.isNotBlank()) holdLiveTranscript else "تحدث الآن... جاري الاستماع وتحويل الكلمات إلى نص",
                        fontSize = 13.sp,
                        fontWeight = if (holdLiveTranscript.isNotBlank()) FontWeight.Bold else FontWeight.Normal,
                        color = if (holdLiveTranscript.isNotBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AttachedDocumentBanner(
    doc: DocumentInfo?,
    onClear: () -> Unit
) {
    AnimatedVisibility(
        visible = doc != null,
        enter = expandVertically(),
        exit = shrinkVertically()
    ) {
        if (doc != null) {
            val isPdf = doc.fileName.endsWith(".pdf", true)
            val isWord = doc.fileName.endsWith(".doc", true) || doc.fileName.endsWith(".docx", true)
            val isExcel = doc.fileName.endsWith(".xls", true) || doc.fileName.endsWith(".xlsx", true) || doc.fileName.endsWith(".csv", true)
            val isArchive = doc.fileName.endsWith(".zip", true) || doc.fileName.endsWith(".rar", true)
            val docBadgeColor = when {
                isPdf -> Color(0xFFEF4444)
                isWord -> Color(0xFF2563EB)
                isExcel -> Color(0xFF10B981)
                isArchive -> Color(0xFFF59E0B)
                else -> GoldPrimary
            }
            val docIcon = when {
                isPdf -> Icons.Default.PictureAsPdf
                isWord -> Icons.Default.Description
                isExcel -> Icons.Default.TableChart
                isArchive -> Icons.Default.FolderZip
                else -> Icons.Default.InsertDriveFile
            }

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, docBadgeColor.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            color = docBadgeColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = docIcon,
                                    contentDescription = null,
                                    tint = docBadgeColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = doc.fileName,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = docBadgeColor.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = FileUtils.getFileExtension(doc.fileName).uppercase().ifBlank { "DOC" },
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = docBadgeColor,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = FileUtils.formatFileSize(doc.fileSize),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = onClear,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إلغاء إرفاق المستند",
                            tint = StatusRed
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AttachedMediaBanner(
    capturedImageUri: Uri?,
    mediaProcessProgress: Float,
    textInput: String,
    currentUser: UserEntity?,
    onOpenMediaPreview: (MediaPreviewItem) -> Unit,
    onOpenVideoEditor: (Uri) -> Unit,
    onOpenImageFrameEditor: (Uri) -> Unit,
    onClear: () -> Unit
) {
    val context = LocalContext.current
    AnimatedVisibility(
        visible = capturedImageUri != null,
        enter = expandVertically(),
        exit = shrinkVertically()
    ) {
        val uri = capturedImageUri ?: return@AnimatedVisibility
        val isAttachedVideo = FileUtils.isVideoUri(context, uri)
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            Row(
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    OozoxThumbnailImage(
                        model = uri,
                        width = 52.dp,
                        height = 52.dp,
                        shape = RoundedCornerShape(10.dp),
                        contentScale = ContentScale.Fit,
                        contentDescription = if (isAttachedVideo) "فيديو مرفق" else "صورة مرفقة",
                        onClick = {
                            onOpenMediaPreview(
                                MediaPreviewItem(
                                    uri = uri.toString(),
                                    caption = textInput,
                                    senderName = currentUser?.name ?: "أنا",
                                    timestamp = System.currentTimeMillis(),
                                    isVideo = isAttachedVideo
                                )
                            )
                        }
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (isAttachedVideo) "فيديو جاهز" else "صورة كاملة",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = adaptiveGoldPrimary
                        )
                        Text(
                            text = "${(mediaProcessProgress * 100).roundToInt().coerceIn(0, 100)}%",
                            fontSize = 10.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (isAttachedVideo) {
                        IconButton(
                            onClick = { onOpenVideoEditor(uri) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "تعديل الفيديو",
                                tint = adaptiveGoldPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else {
                        IconButton(
                            onClick = { onOpenImageFrameEditor(uri) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CropFree,
                                contentDescription = "تعديل الصورة",
                                tint = adaptiveGoldPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    IconButton(
                        onClick = onClear,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "حذف الصورة المرفقة",
                            tint = StatusRed,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReplyPreviewBanner(
    replyingToMessage: MessageEntity?,
    onClear: () -> Unit
) {
    AnimatedVisibility(
        visible = replyingToMessage != null,
        enter = expandVertically(),
        exit = shrinkVertically()
    ) {
        if (replyingToMessage != null) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.6f)),
                shadowElevation = 3.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.5.dp)
                                .height(36.dp)
                                .background(GoldPrimary, RoundedCornerShape(2.dp))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Reply,
                            contentDescription = null,
                            tint = GoldPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "الرد على ${replyingToMessage.senderName}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = when {
                                    replyingToMessage.type == MessageType.VOICE.name -> "تسجيل صوتي"
                                    replyingToMessage.type == MessageType.IMAGE.name -> "صورة ملتقطة"
                                    replyingToMessage.type == MessageType.DOCUMENT.name -> "مستند مرفق"
                                    replyingToMessage.type == MessageType.GOLD_PURCHASE.name -> "صفقة ذهب"
                                    replyingToMessage.type == MessageType.INVOICE.name -> "فاتورة معتمدة"
                                    replyingToMessage.type == MessageType.PAYMENT.name -> "إشعار سداد"
                                    else -> if (replyingToMessage.content.startsWith("↩️ رد على ") && replyingToMessage.content.contains("\n\n")) {
                                        replyingToMessage.content.substringAfter("\n\n").take(80)
                                    } else {
                                        replyingToMessage.content.take(80)
                                    }
                                },
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                    IconButton(
                        onClick = onClear,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إلغاء الرد",
                            tint = StatusRed,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MentionSuggestionsStrip(
    textInput: String,
    mentionCandidates: List<String>,
    hideForRecording: Boolean,
    onSelectMention: (String) -> Unit
) {
    val activeMentionMatch = remember(textInput) {
        Regex("(?:^|\\s)@([\\u0600-\\u06FF\\w._-]*)$").find(textInput)
    }
    val filteredMentions = remember(activeMentionMatch, mentionCandidates) {
        val q = activeMentionMatch?.groupValues?.getOrNull(1)?.trim().orEmpty()
        if (activeMentionMatch == null) emptyList()
        else mentionCandidates.filter { candidate ->
            q.isBlank() || candidate.contains(q, ignoreCase = true)
        }.take(6)
    }

    AnimatedVisibility(
        visible = filteredMentions.isNotEmpty() && !hideForRecording,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.94f),
            border = BorderStroke(0.8.dp, GoldPrimary.copy(alpha = 0.45f)),
            shadowElevation = 4.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp)
                .testTag("chat_mention_suggestions_strip")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "@منشن:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = GoldPrimary
                )
                filteredMentions.forEach { candidate ->
                    val handleOnly = candidate.substringBefore(" - ").substringBefore(" (").trim()
                    Surface(
                        onClick = {
                            val match = activeMentionMatch
                            if (match != null) {
                                val prefix = textInput.substring(0, match.range.first)
                                val leadingSpace = if (match.value.startsWith(" ")) " " else ""
                                val cleanHandle = if (handleOnly.startsWith("@")) handleOnly else "@$handleOnly"
                                onSelectMention("$prefix$leadingSpace$cleanHandle ")
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(0.8.dp, GoldPrimary.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = candidate,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ComposerInputRow(
    textInput: String,
    onTextInputChange: (String) -> Unit,
    attachedDocumentInfo: DocumentInfo?,
    capturedImageUri: Uri?,
    replyingToMessage: MessageEntity?,
    isInlineRecordingAudio: Boolean,
    onInlineRecordingAudioChange: (Boolean) -> Unit,
    isInlineRecordCancelZone: Boolean,
    onInlineRecordCancelZoneChange: (Boolean) -> Unit,
    inlineRecordSeconds: Int,
    onInlineRecordSecondsChange: (Int) -> Unit,
    inlineRecordAmp: Float,
    voiceDraftPath: String?,
    onVoiceDraftPathChange: (String?) -> Unit,
    voiceDraftDurationSec: Int,
    onVoiceDraftDurationChange: (Int) -> Unit,
    isDraftAudioPlaying: Boolean,
    onDraftAudioPlayingChange: (Boolean) -> Unit,
    draftAudioProgress: Float,
    onDraftAudioProgressChange: (Float) -> Unit,
    isActionButtonCameraMode: Boolean,
    onToggleActionButtonCameraMode: () -> Unit,
    isImeOpen: Boolean,
    onOpenAttachmentSheet: () -> Unit,
    onClearAttachedDocument: () -> Unit,
    onClearCapturedImage: () -> Unit,
    onClearReplyingTo: () -> Unit,
    onRequestMicPermission: () -> Unit,
    onLaunchFrontVideoNote: () -> Unit,
    onSendMessage: (String, String) -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val hapticFeedback = LocalHapticFeedback.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        OozoxCircleIconButton(
            onClick = onOpenAttachmentSheet,
            imageVector = Icons.Default.Add,
            contentDescription = "إرفاق وأدوات",
            tint = GoldPrimary,
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
            modifier = Modifier.testTag("chat_attachment_plus_btn")
        )

        when {
            isInlineRecordingAudio -> {
                InlineRecordingPill(
                    isInlineRecordCancelZone = isInlineRecordCancelZone,
                    inlineRecordSeconds = inlineRecordSeconds,
                    inlineRecordAmp = inlineRecordAmp,
                    modifier = Modifier.weight(1f)
                )
            }

            voiceDraftPath != null -> {
                VoiceDraftPlayerPill(
                    voiceDraftPath = voiceDraftPath,
                    voiceDraftDurationSec = voiceDraftDurationSec,
                    isDraftAudioPlaying = isDraftAudioPlaying,
                    draftAudioProgress = draftAudioProgress,
                    onDelete = {
                        if (isDraftAudioPlaying) {
                            AudioPlayerManager.stop()
                            onDraftAudioPlayingChange(false)
                        }
                        val path = voiceDraftPath
                        onVoiceDraftPathChange(null)
                        onVoiceDraftDurationChange(0)
                        onDraftAudioProgressChange(0f)
                        try { File(path).delete() } catch (_: Exception) {}
                    },
                    onTogglePlay = {
                        if (isDraftAudioPlaying) {
                            AudioPlayerManager.stop()
                            onDraftAudioPlayingChange(false)
                        } else {
                            onDraftAudioPlayingChange(true)
                            AudioPlayerManager.play(
                                context = context,
                                pathOrUri = voiceDraftPath,
                                onProgress = { cur, tot ->
                                    if (tot > 0) {
                                        onDraftAudioProgressChange((cur.toFloat() / tot.toFloat()).coerceIn(0f, 1f))
                                    }
                                },
                                onComplete = {
                                    onDraftAudioPlayingChange(false)
                                    onDraftAudioProgressChange(0f)
                                }
                            )
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            else -> {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 38.dp, max = 108.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (textInput.isEmpty()) {
                                Text(
                                    text = when {
                                        attachedDocumentInfo != null -> "تعليق..."
                                        capturedImageUri != null -> "تعليق..."
                                        else -> "رسالة..."
                                    },
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            BasicTextField(
                                value = textInput,
                                onValueChange = onTextInputChange,
                                textStyle = TextStyle(
                                    fontSize = 14.5.sp,
                                    lineHeight = 19.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                cursorBrush = SolidColor(GoldPrimary),
                                maxLines = 4,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("chat_message_input")
                            )
                        }
                        if (textInput.isNotBlank()) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .clickable { onTextInputChange("") },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "مسح النص",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        val hasContentToSend = textInput.isNotBlank() ||
            attachedDocumentInfo != null ||
            capturedImageUri != null ||
            voiceDraftPath != null

        if (hasContentToSend) {
            OozoxCircleIconButton(
                onClick = {
                    when {
                        voiceDraftPath != null -> {
                            if (isDraftAudioPlaying) {
                                AudioPlayerManager.stop()
                                onDraftAudioPlayingChange(false)
                            }
                            val dur = voiceDraftDurationSec.coerceAtLeast(1)
                            val payload = "${voiceDraftPath}||$dur||"
                            onSendMessage(payload, MessageType.VOICE.name)
                            onVoiceDraftPathChange(null)
                            onVoiceDraftDurationChange(0)
                            onDraftAudioProgressChange(0f)
                            onClearReplyingTo()
                        }
                        attachedDocumentInfo != null -> {
                            val doc = attachedDocumentInfo
                            val payload = "${doc.uri}||${doc.fileName}||${doc.fileSize}||${textInput.trim()}"
                            onSendMessage(payload, MessageType.DOCUMENT.name)
                            onClearAttachedDocument()
                            onClearReplyingTo()
                            onTextInputChange("")
                        }
                        capturedImageUri != null -> {
                            val contentPayload = if (textInput.isNotBlank()) {
                                "${capturedImageUri}||${textInput.trim()}"
                            } else {
                                capturedImageUri.toString()
                            }
                            onSendMessage(contentPayload, MessageType.IMAGE.name)
                            onClearCapturedImage()
                            onClearReplyingTo()
                            onTextInputChange("")
                        }
                        textInput.isNotBlank() -> {
                            val sendText = textInput.trim()
                            val finalContent = if (replyingToMessage != null) {
                                val rep = replyingToMessage
                                val repSnippet = when {
                                    rep.type == MessageType.VOICE.name -> "تسجيل صوتي"
                                    rep.type == MessageType.IMAGE.name -> "صورة"
                                    rep.type == MessageType.DOCUMENT.name -> "مستند"
                                    rep.type == MessageType.GOLD_PURCHASE.name -> "صفقة ذهب"
                                    rep.type == MessageType.INVOICE.name -> "فاتورة"
                                    rep.type == MessageType.PAYMENT.name -> "سداد مالي"
                                    else -> {
                                        val c = rep.content
                                        if (c.startsWith("↩️ رد على ") && c.contains("\n\n")) c.substringAfter("\n\n").take(60) else c.take(60)
                                    }
                                }
                                "↩️ رد على ${rep.senderName}:\n\"$repSnippet\"\n\n$sendText"
                            } else {
                                sendText
                            }
                            val tag = HashtagUtils.parseHashtag(sendText)
                            val msgType = if (tag != null) MessageType.HASHTAG.name else MessageType.TEXT.name
                            onSendMessage(finalContent, msgType)
                            onClearReplyingTo()
                            onTextInputChange("")
                        }
                    }
                },
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "إرسال",
                tint = adaptiveOnPrimary,
                containerColor = adaptiveGoldPrimary,
                modifier = Modifier.testTag("chat_send_btn")
            )
        } else {
            val cancelThresholdPx = with(density) { 84.dp.toPx() }
            val swipeDownCameraThresholdPx = with(density) { 22.dp.toPx() }

            Box(
                modifier = Modifier
                    .size(OozoxCircleButtonDefaults.TouchSize)
                    .pointerInput(isActionButtonCameraMode, isImeOpen) {
                        if (isImeOpen) {
                            detectTapGestures(
                                onTap = {
                                    onToggleActionButtonCameraMode()
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            )
                        } else {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                val touchSlop = viewConfiguration.touchSlop
                                var totalDx = 0f
                                var totalDy = 0f
                                var isDragging = false
                                var recordingStarted = false
                                var recStartMs = 0L
                                var cameraTriggered = false

                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.id == down.id }
                                        ?: event.changes.firstOrNull()
                                        ?: break

                                    if (!change.pressed) {
                                        if (recordingStarted) {
                                            onInlineRecordingAudioChange(false)
                                            if (isInlineRecordCancelZone) {
                                                AudioRecorderManager.cancelRecording()
                                                onInlineRecordCancelZoneChange(false)
                                            } else {
                                                val recorded = AudioRecorderManager.stopRecording()
                                                val heldMs = System.currentTimeMillis() - recStartMs
                                                if (recorded != null && recorded.exists() && heldMs >= 320L) {
                                                    onVoiceDraftPathChange(recorded.absolutePath)
                                                    onVoiceDraftDurationChange((heldMs / 1000L).toInt().coerceAtLeast(1))
                                                    onDraftAudioProgressChange(0f)
                                                    onDraftAudioPlayingChange(false)
                                                } else if (recorded != null && recorded.exists()) {
                                                    try { recorded.delete() } catch (_: Exception) {}
                                                }
                                            }
                                        } else if (!isDragging && !cameraTriggered) {
                                            onToggleActionButtonCameraMode()
                                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                        break
                                    }

                                    val delta = change.position - change.previousPosition
                                    totalDx += delta.x
                                    totalDy += delta.y

                                    if (!isDragging && kotlin.math.hypot(totalDx.toDouble(), totalDy.toDouble()) > touchSlop) {
                                        isDragging = true
                                        if (!isActionButtonCameraMode) {
                                            val hasMicPerm = ContextCompat.checkSelfPermission(
                                                context,
                                                Manifest.permission.RECORD_AUDIO
                                            ) == PackageManager.PERMISSION_GRANTED
                                            if (!hasMicPerm) {
                                                onRequestMicPermission()
                                                break
                                            }
                                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                            val file = AudioRecorderManager.startRecording(context)
                                            if (file != null) {
                                                recordingStarted = true
                                                recStartMs = System.currentTimeMillis()
                                                onInlineRecordSecondsChange(0)
                                                onInlineRecordCancelZoneChange(false)
                                                onInlineRecordingAudioChange(true)
                                            }
                                        }
                                    }

                                    if (isDragging) {
                                        change.consume()
                                        if (!isActionButtonCameraMode && recordingStarted) {
                                            val cancelNow = kotlin.math.abs(totalDx) > cancelThresholdPx
                                            if (cancelNow != isInlineRecordCancelZone) {
                                                onInlineRecordCancelZoneChange(cancelNow)
                                                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            }
                                        } else if (isActionButtonCameraMode && !cameraTriggered && totalDy > swipeDownCameraThresholdPx) {
                                            cameraTriggered = true
                                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onLaunchFrontVideoNote()
                                        }
                                    }
                                }
                            }
                        }
                    }
                    .testTag("chat_mic_camera_toggle_btn"),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = when {
                        isInlineRecordingAudio && isInlineRecordCancelZone -> StatusRed
                        isInlineRecordingAudio -> Color(0xFFEF4444)
                        isActionButtonCameraMode -> Color(0xFF10B981)
                        else -> GoldPrimary
                    },
                    modifier = Modifier.size(OozoxCircleButtonDefaults.CircleSize)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = when {
                                isInlineRecordingAudio && isInlineRecordCancelZone -> Icons.Default.Close
                                isInlineRecordingAudio -> Icons.Default.Mic
                                isActionButtonCameraMode -> Icons.Default.CameraAlt
                                else -> Icons.Default.Mic
                            },
                            contentDescription = if (isActionButtonCameraMode) "كاميرا" else "تسجيل صوتي",
                            tint = adaptiveOnPrimary,
                            modifier = Modifier.size(OozoxCircleButtonDefaults.IconSize)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InlineRecordingPill(
    isInlineRecordCancelZone: Boolean,
    inlineRecordSeconds: Int,
    inlineRecordAmp: Float,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = if (isInlineRecordCancelZone) {
            StatusRed.copy(alpha = 0.16f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
        },
        border = BorderStroke(
            1.dp,
            if (isInlineRecordCancelZone) StatusRed else adaptiveGoldPrimary.copy(alpha = 0.55f)
        ),
        modifier = modifier
            .height(42.dp)
            .testTag("inline_recording_pill")
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(if (isInlineRecordCancelZone) StatusRed else Color(0xFFEF4444))
            )
            Text(
                text = String.format(
                    Locale.US,
                    "%02d:%02d",
                    inlineRecordSeconds / 60,
                    inlineRecordSeconds % 60
                ),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (isInlineRecordCancelZone) StatusRed else MaterialTheme.colorScheme.onSurface
            )
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(22.dp),
                horizontalArrangement = Arrangement.spacedBy(2.5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(14) { idx ->
                    val mod = ((idx * 5) % 6 + 3) / 8f
                    val h = (4f + inlineRecordAmp * 16f * mod).dp
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(h)
                            .clip(CircleShape)
                            .background(
                                if (isInlineRecordCancelZone) StatusRed else adaptiveGoldPrimary
                            )
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.DeleteOutline,
                contentDescription = "حذف التسجيل",
                tint = if (isInlineRecordCancelZone) StatusRed else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(19.dp)
            )
        }
    }
}

@Composable
private fun VoiceDraftPlayerPill(
    voiceDraftPath: String,
    voiceDraftDurationSec: Int,
    isDraftAudioPlaying: Boolean,
    draftAudioProgress: Float,
    onDelete: () -> Unit,
    onTogglePlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        border = BorderStroke(1.dp, adaptiveGoldPrimary.copy(alpha = 0.55f)),
        modifier = modifier
            .height(42.dp)
            .testTag("voice_draft_player_pill")
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(32.dp)
                    .testTag("voice_draft_delete_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "حذف التسجيل الصوتي",
                    tint = StatusRed,
                    modifier = Modifier.size(19.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(adaptiveGoldPrimary)
                    .clickable(onClick = onTogglePlay)
                    .testTag("voice_draft_play_btn"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isDraftAudioPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isDraftAudioPlaying) "إيقاف" else "استماع",
                    tint = adaptiveOnPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }

            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(22.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val totalBars = 18
                repeat(totalBars) { idx ->
                    val barFrac = idx.toFloat() / totalBars.toFloat()
                    val barH = (5 + ((idx * 7) % 13)).dp
                    val passed = barFrac <= draftAudioProgress
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(barH)
                            .clip(CircleShape)
                            .background(
                                if (passed) adaptiveGoldPrimary else adaptiveGoldPrimary.copy(alpha = 0.3f)
                            )
                    )
                }
            }

            Text(
                text = String.format(
                    Locale.US,
                    "%d:%02d",
                    voiceDraftDurationSec / 60,
                    voiceDraftDurationSec % 60
                ),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(end = 6.dp)
            )
        }
    }
}
