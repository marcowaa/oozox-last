package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import com.example.data.model.*
import com.example.ui.components.OozoxAvatarImage
import com.example.ui.components.OozoxThumbnailImage
import com.example.ui.components.SecurityVerificationDialog
import com.example.ui.spatial.CommandCenterDialog
import com.example.ui.spatial.SpatialElevation
import com.example.ui.spatial.SpatialSurface
import com.example.ui.spatial.spatialAmbientBackground
import com.example.ui.theme.*
import com.example.ui.GlobalThemeToggleButton
import com.example.ui.components.ExecutiveChatEmptyState
import com.example.ui.components.ChatEmptyStateType
import com.example.ui.components.FeatureGatedDialog
import com.example.ui.components.SubscriptionStatusTopBanner
import com.example.ui.components.SyncStatus
import com.example.util.*

private data class PendingChatUndoAction(
    val chatId: String,
    val label: String,
    val isDelete: Boolean,
    val previousArchived: Boolean
)

private data class AnchoredChatMenuState(
    val chat: ChatEntity,
    val anchorTopPx: Float,
    val anchorHeightPx: Float
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    chats: List<ChatEntity>,
    currentUser: UserEntity? = null,
    adminConfig: AdminConfigEntity? = null,
    goldPrices: GoldPricesModel = GoldUtils.DEFAULT_GOLD_PRICES,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
    lastSyncTime: Long? = null,
    activeHiddenProfile: HiddenProfileEntity? = null,
    onUnlockHiddenProfile: (String) -> HiddenProfileEntity? = { null },
    onLockHiddenProfile: () -> Unit = {},
    activeSubAccount: HiddenSubAccountEntity? = null,
    onUnlockSubAccount: (String) -> HiddenSubAccountEntity? = { null },
    onLockSubAccount: () -> Unit = {},
    onCreateParallelSubAccount: (username: String, displayName: String, passwordSecret: String, mute: Boolean, discreet: Boolean) -> Unit = { _, _, _, _, _ -> },
    onOpenCompanyAccounting: () -> Unit = {},
    onOpenSubscriptions: () -> Unit = {},
    onOpenDrawer: (() -> Unit)? = null,
    isTopHeaderVisible: Boolean = true,
    isBottomBarVisible: Boolean = true,
    nestedScrollConnection: NestedScrollConnection? = null,
    hashtagEntries: List<HashtagEntryEntity> = emptyList(),
    ledgerEntries: List<LedgerEntryEntity> = emptyList(),
    settlements: List<ClearingSettlementEntity> = emptyList(),
    onChatClick: (String) -> Unit,
    onNewChatClick: () -> Unit,
    onCreateGroupFolder: (groupName: String, selectedChatIds: List<String>) -> Unit = { _, _ -> },
    onRemoveGroupFolder: (groupName: String) -> Unit = {},
    onRenameGroupFolder: (oldName: String, newName: String, selectedChatIds: List<String>?) -> Unit = { _, _, _ -> },
    onOpenAIAccountant: () -> Unit,
    onOpenGoldDealClick: () -> Unit = {},
    onOpenNewProjectClick: () -> Unit = {},
    onTogglePinChat: (chatId: String, isPinned: Boolean) -> Unit = { _, _ -> },
    onArchiveChat: (chatId: String, isArchived: Boolean) -> Unit = { _, _ -> },
    onDeleteChat: (chatId: String) -> Unit = {},
    onMarkChatAsRead: (chatId: String) -> Unit = {},
    workGroups: List<WorkGroupEntity> = emptyList(),
    projects: List<ProjectEntity> = emptyList(),
    onAssignToProjectWorkGroup: (
        chat: ChatEntity,
        workGroup: WorkGroupEntity,
        project: ProjectEntity?,
        roleLabel: String,
        projectRole: ProjectMemberRole,
        canPostAccounting: Boolean,
        canEditRecords: Boolean,
        canExportReports: Boolean,
        canMessageDirectly: Boolean,
        isGroupAdmin: Boolean
    ) -> Unit = { _, _, _, _, _, _, _, _, _, _ -> },
    contacts: List<ContactEntity> = emptyList(),
    onCreateStandaloneWorkGroup: (
        groupName: String,
        description: String?,
        selectedContacts: List<ContactEntity>,
        disableMemberDMs: Boolean,
        onCreated: (String) -> Unit
    ) -> Unit = { _, _, _, _, _ -> }
) {
    val haptic = LocalHapticFeedback.current
    val context = androidx.compose.ui.platform.LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    val searchFocusRequester = remember { FocusRequester() }
    var unlockedProfileSuccess by remember { mutableStateOf<HiddenProfileEntity?>(null) }
    var featureGatedDecision by remember { mutableStateOf<FeatureAccessDecision?>(null) }
    var selectedTypeFilter by remember { mutableStateOf<String?>(null) }
    var selectedGroupFilter by remember { mutableStateOf<String?>(null) }
    var showOnlyUnreadFilter by remember { mutableStateOf(false) }
    var showArchivedOnly by remember { mutableStateOf(false) }
    var showCommandCenter by remember { mutableStateOf(false) }
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var showCreateWorkGroupDialog by remember { mutableStateOf(false) }
    var topGroupConfigs by remember { mutableStateOf(ChatFolderPrefs.loadTopGroups(context)) }
    var editingTopGroupTarget by remember { mutableStateOf<TopChatGroupConfig?>(null) }
    var editingCustomFolderName by remember { mutableStateOf<String?>(null) }
    var showManageTopGroupsDialog by remember { mutableStateOf(false) }
    var showPlusMenuBottomSheet by remember { mutableStateOf(false) }
    var showMoreOptionsMenu by remember { mutableStateOf(false) }
    var anchoredChatMenu by remember { mutableStateOf<AnchoredChatMenuState?>(null) }
    var chatToAssignWorkGroup by remember { mutableStateOf<ChatEntity?>(null) }
    var rootHeightPx by remember { mutableFloatStateOf(0f) }
    var rootTopYInWindow by remember { mutableFloatStateOf(0f) }
    var chatToDeleteConfirm by remember { mutableStateOf<ChatEntity?>(null) }
    var chatToBlockConfirm by remember { mutableStateOf<ChatEntity?>(null) }
    var pendingDeleteChatIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var pendingUndoAction by remember { mutableStateOf<PendingChatUndoAction?>(null) }

    LaunchedEffect(pendingUndoAction) {
        val action = pendingUndoAction ?: return@LaunchedEffect
        delay(1200L)
        if (pendingUndoAction == action) {
            if (action.isDelete) {
                pendingDeleteChatIds = pendingDeleteChatIds - action.chatId
                onDeleteChat(action.chatId)
            }
            pendingUndoAction = null
        }
    }

    val customGroups = remember(chats) {
        chats.mapNotNull { it.groupName }.filter { it.isNotBlank() }.distinct()
    }

    LaunchedEffect(customGroups) {
        topGroupConfigs = ChatFolderPrefs.syncWithCustomFolders(context, customGroups)
    }

    val unreadChatsCount = remember(chats) {
        chats.count { it.unreadCount > 0 && !it.isArchived }
    }

    val archivedChatsCount = remember(chats) {
        chats.count { it.isArchived }
    }

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val filteredChats = remember(chats, pendingDeleteChatIds, searchQuery, selectedTypeFilter, selectedGroupFilter, showOnlyUnreadFilter, showArchivedOnly) {
        chats.filter { chat ->
            if (chat.id in pendingDeleteChatIds) return@filter false
            val matchesArchive = if (showArchivedOnly) chat.isArchived else !chat.isArchived
            val matchesSearch = searchQuery.isBlank() ||
                    chat.contactName.contains(searchQuery, ignoreCase = true) ||
                    (chat.lastMessage?.contains(searchQuery, ignoreCase = true) == true) ||
                    (chat.groupName?.contains(searchQuery, ignoreCase = true) == true)

            val matchesType = selectedTypeFilter == null || chat.contactType == selectedTypeFilter
            val matchesGroup = selectedGroupFilter == null || chat.groupName == selectedGroupFilter
            val matchesUnread = !showOnlyUnreadFilter || chat.unreadCount > 0

            matchesArchive && matchesSearch && matchesType && matchesGroup && matchesUnread
        }
    }

    // Panic Exit: Immediate lock & return to main account when Back is pressed in Hidden Sub-Account mode
    BackHandler(enabled = activeSubAccount != null) {
        onLockSubAccount()
    }

    // Matching hashtag and financial operations in real time
    val matchingHashtagEntries = remember(searchQuery, hashtagEntries) {
        if (searchQuery.isBlank()) emptyList()
        else {
            val queryClean = searchQuery.trim().removePrefix("#").lowercase()
            hashtagEntries.filter { entry ->
                entry.description.contains(queryClean, ignoreCase = true) ||
                entry.hashtagId.contains(queryClean, ignoreCase = true) ||
                searchQuery.startsWith("#")
            }
        }
    }

    val hashtagTotals = remember(matchingHashtagEntries) {
        val debit = matchingHashtagEntries.filter { it.direction == "DEBIT" || it.direction.contains("مدين", ignoreCase = true) }.sumOf { it.amount }
        val credit = matchingHashtagEntries.filter { it.direction != "DEBIT" && !it.direction.contains("مدين", ignoreCase = true) }.sumOf { it.amount }
        val balance = debit - credit
        Triple(debit, credit, balance)
    }

    var isScrollHeaderVisible by remember { mutableStateOf(true) }

    val localScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                if (delta < -12f && isScrollHeaderVisible && searchQuery.isBlank()) {
                    isScrollHeaderVisible = false
                } else if (delta > 12f && !isScrollHeaderVisible) {
                    isScrollHeaderVisible = true
                }
                return Offset.Zero
            }
        }
    }

    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotBlank()) {
            isScrollHeaderVisible = true
        }
        if (searchQuery.isNotBlank() && searchQuery.length >= 2) {
            val matchedSub = onUnlockSubAccount(searchQuery.trim())
            if (matchedSub != null) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                searchQuery = ""
                isSearchActive = false
            }
        }
    }

    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            try {
                searchFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    val combinedScrollModifier = if (nestedScrollConnection != null) {
        Modifier.nestedScroll(nestedScrollConnection).nestedScroll(localScrollConnection)
    } else {
        Modifier.nestedScroll(localScrollConnection)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .spatialAmbientBackground()
            .onGloballyPositioned { coords ->
                rootHeightPx = coords.size.height.toFloat()
                rootTopYInWindow = coords.positionInWindow().y
            }
            .then(combinedScrollModifier)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Animated Header (Telegram-Style Header with Scroll Collapsing for Search and Sections)
            AnimatedVisibility(
                visible = isTopHeaderVisible,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
            ) {
                Surface(
                    color = if (isDark) MaterialTheme.colorScheme.surface.copy(alpha = 0.98f) else Color(0xFFEFF3F8),
                    tonalElevation = SpatialElevation.Level1,
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.statusBars)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        // Collapsible Header (Top Bar, Live Gold Ticker & Search Bar — hides on scroll down, reappears on scroll up)
                        AnimatedVisibility(
                            visible = isScrollHeaderVisible || searchQuery.isNotBlank(),
                            enter = expandVertically(animationSpec = tween(220)) + fadeIn(animationSpec = tween(200)),
                            exit = shrinkVertically(animationSpec = tween(180)) + fadeOut(animationSpec = tween(150))
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                        // Top Clean Navigation Bar (Unified & Minimalist Telegram-Style)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Start (RTL): Drawer Menu + Official Brand Logo
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                if (onOpenDrawer != null) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                            .border(1.dp, adaptiveGoldPrimary.copy(alpha = 0.35f), CircleShape)
                                            .clickable { onOpenDrawer() }
                                            .testTag("chat_list_open_drawer_button"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Menu,
                                            contentDescription = "القائمة الجانبية",
                                            tint = adaptiveGoldPrimary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                OozoxLogoHeader()
                            }

                            // End (RTL): Actions (SyncStatus + Search Icon + Unified Clean Action Menu)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Background Sync Status Indicator
                                SyncStatus(
                                    onClick = {
                                        OfflineSyncManager.triggerImmediateSync(context)
                                    }
                                )

                                // Dedicated Gold Section Top Action Button
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                                        .border(1.dp, GoldPrimary.copy(alpha = 0.35f), CircleShape)
                                        .clickable { onOpenGoldDealClick() }
                                        .testTag("chat_list_gold_top_button"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MonetizationOn,
                                        contentDescription = "قسم الذهب",
                                        tint = GoldPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                // Top Search Icon Button
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSearchActive) GoldPrimary.copy(alpha = 0.22f)
                                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                                        )
                                        .border(1.dp, GoldPrimary.copy(alpha = if (isSearchActive) 0.7f else 0.35f), CircleShape)
                                        .clickable {
                                            isScrollHeaderVisible = true
                                            if (isSearchActive) {
                                                isSearchActive = false
                                                searchQuery = ""
                                            } else {
                                                isSearchActive = true
                                            }
                                        }
                                        .testTag("chat_list_search_top_button"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "بحث",
                                        tint = GoldPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                // More Options Menu
                                Box {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                                            .border(1.dp, GoldPrimary.copy(alpha = 0.35f), CircleShape)
                                            .clickable { showMoreOptionsMenu = true }
                                            .testTag("chat_list_more_menu_button"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MoreVert,
                                            contentDescription = "خيارات المنصة والمحاسبة",
                                            tint = GoldPrimary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }

                                    DropdownMenu(
                                        expanded = showMoreOptionsMenu,
                                        onDismissRequest = { showMoreOptionsMenu = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("المحاسب المالي", fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold) },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Default.AutoAwesome,
                                                    contentDescription = null,
                                                    tint = Color(0xFFA78BFA),
                                                    modifier = Modifier.size(19.dp)
                                                )
                                            },
                                            onClick = {
                                                showMoreOptionsMenu = false
                                                val check = SubscriptionFeatureGate.checkFeatureAccess(currentUser, AppFeature.GEMINI_AI_AND_OCR)
                                                if (check.isAllowed) {
                                                    onOpenAIAccountant()
                                                } else {
                                                    featureGatedDecision = check
                                                }
                                            }
                                        )

                                        DropdownMenuItem(
                                            text = { Text("المركز المحاسبي", fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold) },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Default.CorporateFare,
                                                    contentDescription = null,
                                                    tint = GoldPrimary,
                                                    modifier = Modifier.size(19.dp)
                                                )
                                            },
                                            onClick = {
                                                showMoreOptionsMenu = false
                                                onOpenCompanyAccounting()
                                            }
                                        )

                                        DropdownMenuItem(
                                            text = { Text("الفقاعة العائمة", fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold) },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Default.ChatBubble,
                                                    contentDescription = null,
                                                    tint = GoldPrimary,
                                                    modifier = Modifier.size(19.dp)
                                                )
                                            },
                                            onClick = {
                                                showMoreOptionsMenu = false
                                                val firstChat = chats.firstOrNull()
                                                if (firstChat != null) {
                                                    com.example.util.MessengerBubbleManager.showBubble(
                                                        chatId = firstChat.id,
                                                        contactName = firstChat.contactName,
                                                        contactAvatarUri = null,
                                                        contactRole = if (firstChat.contactType == "GOLD_TRADER") "تاجر ذهب معتمد" else "حساب أعمال",
                                                        messageText = "مرحباً بك في المحادثة الفورية",
                                                        autoExpand = true,
                                                        accountType = if (firstChat.contactType == "GOLD_TRADER") com.example.util.BubbleAccountType.BUSINESS else com.example.util.BubbleAccountType.PERSONAL
                                                    )
                                                } else {
                                                    android.widget.Toast.makeText(context, "يرجى بدء محادثة أولاً لتشغيل الفقاعة العائمة", android.widget.Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        )

                                        HorizontalDivider(
                                            modifier = Modifier.padding(vertical = 4.dp),
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                        )

                                        DropdownMenuItem(
                                            text = { Text("قسم الذهب والمعادن", fontSize = 13.sp) },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Default.MonetizationOn,
                                                    contentDescription = null,
                                                    tint = GoldAccent,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            },
                                            onClick = {
                                                showMoreOptionsMenu = false
                                                onOpenGoldDealClick()
                                            }
                                        )

                                        DropdownMenuItem(
                                            text = { Text("مجلد جديد", fontSize = 13.sp) },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Default.CreateNewFolder,
                                                    contentDescription = null,
                                                    tint = GoldPrimary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            },
                                            onClick = {
                                                showMoreOptionsMenu = false
                                                showCreateGroupDialog = true
                                            }
                                        )

                                        DropdownMenuItem(
                                            text = { Text("لوحة الأوامر", fontSize = 13.sp) },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Default.Terminal,
                                                    contentDescription = null,
                                                    tint = GoldPrimary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            },
                                            onClick = {
                                                showMoreOptionsMenu = false
                                                showCommandCenter = true
                                            }
                                        )
                                    }
                                }
                            }
                        }

                                AnimatedVisibility(
                                    visible = isSearchActive || searchQuery.isNotEmpty(),
                                    enter = expandVertically(animationSpec = tween(200)) + fadeIn(animationSpec = tween(180)),
                                    exit = shrinkVertically(animationSpec = tween(180)) + fadeOut(animationSpec = tween(150))
                                ) {
                                    Column {
                                        Spacer(modifier = Modifier.height(6.dp))

                                        // Compact Search Field (~15% shorter vertically)
                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.30f),
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = if (searchQuery.isNotEmpty()) adaptiveGoldPrimary.copy(alpha = 0.7f) else Color.Transparent
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = "بحث",
                                            tint = adaptiveGoldPrimary,
                                            modifier = Modifier.size(19.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        androidx.compose.foundation.text.BasicTextField(
                                            value = searchQuery,
                                            onValueChange = { newQuery ->
                                                searchQuery = newQuery
                                                if (newQuery.isNotBlank() && newQuery.length >= 2) {
                                                    val matchedSub = onUnlockSubAccount(newQuery.trim())
                                                    if (matchedSub != null) {
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        searchQuery = ""
                                                        isSearchActive = false
                                                    }
                                                }
                                            },
                                            singleLine = true,
                                            textStyle = LocalTextStyle.current.copy(
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            ),
                                            cursorBrush = androidx.compose.ui.graphics.SolidColor(adaptiveGoldPrimary),
                                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                                imeAction = androidx.compose.ui.text.input.ImeAction.Search
                                            ),
                                            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                                                onSearch = {
                                                    val trimmed = searchQuery.trim()
                                                    if (trimmed.isNotBlank()) {
                                                        val matchedSub = onUnlockSubAccount(trimmed)
                                                        if (matchedSub != null) {
                                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                            searchQuery = ""
                                                            isSearchActive = false
                                                        }
                                                    }
                                                }
                                            ),
                                            decorationBox = { innerTextField ->
                                                Box(
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentAlignment = Alignment.CenterStart
                                                ) {
                                                    if (searchQuery.isEmpty()) {
                                                        Text(
                                                            text = "بحث...",
                                                            fontSize = 12.sp,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                    innerTextField()
                                                }
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .heightIn(min = 48.dp)
                                                .focusRequester(searchFocusRequester)
                                        )
                                        IconButton(
                                            onClick = {
                                                if (searchQuery.isNotEmpty()) {
                                                    searchQuery = ""
                                                } else {
                                                    isSearchActive = false
                                                }
                                            },
                                            modifier = Modifier.size(48.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Clear,
                                                contentDescription = if (searchQuery.isNotEmpty()) "مسح" else "إغلاق البحث",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                    }
                                }
                            }
                        }

                                 // Category / Topic Pills Row (Every group can be edited, deleted, or created)
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState())
                                ) {
                                    val isAllSelected = selectedTypeFilter == null && selectedGroupFilter == null && !showOnlyUnreadFilter && !showArchivedOnly
                                    FilterChip(
                                        selected = isAllSelected,
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            selectedTypeFilter = null
                                            selectedGroupFilter = null
                                            showOnlyUnreadFilter = false
                                            showArchivedOnly = false
                                        },
                                        leadingIcon = if (isAllSelected) {
                                            {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        } else null,
                                        label = {
                                            Text(
                                                "الكل (${chats.count { !it.isArchived }})",
                                                fontSize = 12.sp,
                                                fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = adaptiveGoldPrimary,
                                            selectedLabelColor = adaptiveOnPrimary,
                                            selectedLeadingIconColor = adaptiveOnPrimary,
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                            labelColor = MaterialTheme.colorScheme.onSurface
                                        ),
                                        shape = RoundedCornerShape(16.dp)
                                    )

                                    val orderedVisibleGroups = remember(
                                        topGroupConfigs,
                                        customGroups,
                                        unreadChatsCount,
                                        archivedChatsCount,
                                        showOnlyUnreadFilter,
                                        showArchivedOnly,
                                        chats
                                    ) {
                                        topGroupConfigs.filter { cfg ->
                                            if (cfg.isHidden) return@filter false
                                            if (cfg.isBuiltIn) {
                                                when (cfg.id) {
                                                    ChatFolderPrefs.KEY_UNREAD -> unreadChatsCount > 0 || showOnlyUnreadFilter
                                                    ChatFolderPrefs.KEY_ARCHIVED -> archivedChatsCount > 0 || showArchivedOnly
                                                    ChatFolderPrefs.KEY_GOLD_TRADER -> chats.any { it.contactType == UserType.GOLD_TRADER.name }
                                                    ChatFolderPrefs.KEY_BUSINESSMAN -> chats.any { it.contactType == UserType.BUSINESSMAN.name }
                                                    else -> true
                                                }
                                            } else {
                                                cfg.filterKey in customGroups
                                            }
                                        }
                                    }

                                    orderedVisibleGroups.forEach { cfg ->
                                        if (cfg.isBuiltIn) {
                                            val isSelected = when (cfg.id) {
                                                ChatFolderPrefs.KEY_UNREAD -> showOnlyUnreadFilter
                                                ChatFolderPrefs.KEY_ARCHIVED -> showArchivedOnly
                                                ChatFolderPrefs.KEY_GOLD_TRADER -> selectedTypeFilter == UserType.GOLD_TRADER.name
                                                ChatFolderPrefs.KEY_BUSINESSMAN -> selectedTypeFilter == UserType.BUSINESSMAN.name
                                                else -> false
                                            }
                                            val chipLabel = when (cfg.id) {
                                                ChatFolderPrefs.KEY_UNREAD -> "${cfg.label} ($unreadChatsCount)"
                                                ChatFolderPrefs.KEY_ARCHIVED -> "${cfg.label} ($archivedChatsCount)"
                                                else -> cfg.label
                                            }
                                            TopEditableGroupChip(
                                                label = chipLabel,
                                                isSelected = isSelected,
                                                isDark = isDark,
                                                onClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    when (cfg.id) {
                                                        ChatFolderPrefs.KEY_UNREAD -> {
                                                            showOnlyUnreadFilter = !showOnlyUnreadFilter
                                                            if (showOnlyUnreadFilter) {
                                                                selectedTypeFilter = null
                                                                selectedGroupFilter = null
                                                                showArchivedOnly = false
                                                            }
                                                        }
                                                        ChatFolderPrefs.KEY_ARCHIVED -> {
                                                            showArchivedOnly = !showArchivedOnly
                                                            if (showArchivedOnly) {
                                                                showOnlyUnreadFilter = false
                                                                selectedTypeFilter = null
                                                                selectedGroupFilter = null
                                                            }
                                                        }
                                                        ChatFolderPrefs.KEY_GOLD_TRADER -> {
                                                            val key = UserType.GOLD_TRADER.name
                                                            showOnlyUnreadFilter = false
                                                            showArchivedOnly = false
                                                            selectedGroupFilter = null
                                                            selectedTypeFilter = if (selectedTypeFilter == key) null else key
                                                        }
                                                        ChatFolderPrefs.KEY_BUSINESSMAN -> {
                                                            val key = UserType.BUSINESSMAN.name
                                                            showOnlyUnreadFilter = false
                                                            showArchivedOnly = false
                                                            selectedGroupFilter = null
                                                            selectedTypeFilter = if (selectedTypeFilter == key) null else key
                                                        }
                                                    }
                                                },
                                                onLongClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    editingTopGroupTarget = cfg
                                                }
                                            )
                                        } else {
                                            val groupName = cfg.filterKey
                                            val isSelected = selectedGroupFilter == groupName
                                            TopEditableGroupChip(
                                                label = cfg.label,
                                                isSelected = isSelected,
                                                isDark = isDark,
                                                onClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    selectedTypeFilter = null
                                                    showOnlyUnreadFilter = false
                                                    showArchivedOnly = false
                                                    selectedGroupFilter = if (isSelected) null else groupName
                                                },
                                                onLongClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    editingCustomFolderName = groupName
                                                }
                                            )
                                        }
                                    }

                                    // Single '+' Button for full Group Management (Create / Edit / Delete / Reorder)
                                    IconButton(
                                        onClick = { showManageTopGroupsDialog = true },
                                        modifier = Modifier
                                            .size(48.dp)
                                            .testTag("manage_chat_groups_plus_btn")
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = GoldPrimary.copy(alpha = 0.16f),
                                            border = BorderStroke(0.9.dp, GoldPrimary.copy(alpha = 0.45f)),
                                            modifier = Modifier.size(34.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.Add,
                                                    contentDescription = "إدارة المجموعات",
                                                    tint = GoldPrimary,
                                                    modifier = Modifier.size(19.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                        // Active Hidden Vault Banner
                        if (activeHiddenProfile != null) {
                            Surface(
                                color = if (isDark) GoldDark.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.2.dp, GoldPrimary),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Security, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text("خزينة سرية نشطة: ${activeHiddenProfile.name}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isDark) GoldLight else MaterialTheme.colorScheme.onSurface)
                                            Text("المؤسسة: ${activeHiddenProfile.companyName} | لا توجد إشعارات", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        IconButton(
                                            onClick = onOpenCompanyAccounting,
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(Icons.Default.TableView, contentDescription = "دفاتر المحاسبة", tint = StatusGreen, modifier = Modifier.size(18.dp))
                                        }

                                        Button(
                                            onClick = onLockHiddenProfile,
                                            colors = ButtonDefaults.buttonColors(containerColor = StatusRed),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("قفل", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        // Active Hidden Parallel Sub-Account Banner (حساب oozox الفرعي المستقل)
                        if (activeSubAccount != null) {
                            Surface(
                                color = if (isDark) GoldDark.copy(alpha = 0.55f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.2.dp, GoldPrimary),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(GoldPrimary.copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.VpnKey,
                                                contentDescription = null,
                                                tint = GoldPrimary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = "الحساب الفرعي المخفي: ${activeSubAccount.displayName}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDark) GoldLight else MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "@${activeSubAccount.username} | ${if (activeSubAccount.muteNotifications) "إشعارات مكتومة" else "إشعارات مموهة"} | مشفر",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Button(
                                        onClick = onLockSubAccount,
                                        colors = ButtonDefaults.buttonColors(containerColor = StatusRed),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("قفل ورجوع", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // Active Sync Banner
                        AnimatedVisibility(
                            visible = isRefreshing,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Surface(
                                color = if (isDark) GoldDark.copy(alpha = 0.35f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        color = GoldPrimary,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "جاري مزامنة المحادثات وتحديث أسعار الذهب والفضة...",
                                        fontSize = 11.sp,
                                        color = if (isDark) GoldLight else MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        // Clean Minimal Search Field removed (now unified seamlessly in Top Bar)

                        // Real-Time Hashtag & Financial Accounting Operations Result Card
                        AnimatedVisibility(
                            visible = searchQuery.isNotBlank() && (searchQuery.contains("#") || matchingHashtagEntries.isNotEmpty()),
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.6f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Tag,
                                                contentDescription = null,
                                                tint = GoldPrimary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "محاسبة الهاشتاج: ${if (searchQuery.startsWith("#")) searchQuery else "#$searchQuery"}",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDark) GoldLight else MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        // Excel Export Button
                                        Surface(
                                            onClick = {
                                                val file = SimpleExcelEngine.exportHashtagReportToExcelCsv(
                                                    context = context,
                                                    hashtagTag = searchQuery,
                                                    entries = matchingHashtagEntries
                                                )
                                                if (file != null) {
                                                    SimpleExcelEngine.shareExcelFile(context, file, "تصدير كشف حساب الهاشتاج Excel")
                                                } else {
                                                    android.widget.Toast.makeText(context, "لا توجد قيود مسجلة لهذا الهاشتاج بعد للتصدير", android.widget.Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            color = StatusGreen.copy(alpha = 0.2f),
                                            border = BorderStroke(1.dp, StatusGreen.copy(alpha = 0.6f))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.FileDownload,
                                                    contentDescription = null,
                                                    tint = StatusGreen,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "تصدير Excel",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = StatusGreen
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Metrics Summary Row
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
                                            .padding(8.dp),
                                        horizontalArrangement = Arrangement.SpaceAround
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("مدين (+)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(GoldUtils.formatCurrency(hashtagTotals.first), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = StatusGreen)
                                        }
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("دائن (-)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(GoldUtils.formatCurrency(hashtagTotals.second), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = StatusRed)
                                        }
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("الرصيد الصافي", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(
                                                GoldUtils.formatCurrency(hashtagTotals.third),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (hashtagTotals.third >= 0) GoldPrimary else StatusRed
                                            )
                                        }
                                    }

                                    if (matchingHashtagEntries.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "الحركات والقيود المرتبطة (${matchingHashtagEntries.size}):",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            matchingHashtagEntries.take(4).forEach { entry ->
                                                val isDebit = entry.direction == "DEBIT" || entry.direction.contains("مدين", ignoreCase = true)
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(
                                                                text = entry.description,
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Medium,
                                                                color = MaterialTheme.colorScheme.onSurface,
                                                                maxLines = 1
                                                            )
                                                            Text(
                                                                text = java.text.SimpleDateFormat("yyyy/MM/dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(entry.createdAt)),
                                                                fontSize = 9.sp,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                        Text(
                                                            text = (if (isDebit) "+" else "-") + GoldUtils.formatCurrency(entry.amount),
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (isDebit) StatusGreen else StatusRed
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "اكتب مثل #حديد 5000 أو #دفعة 2000 في أي محادثة ليتم تسجيل القيد وإضافته للإكسل آلياً.",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Chat List Stream with Pull-to-Refresh
            val pullToRefreshState = rememberPullToRefreshState()
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onRefresh()
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("chat_list_pull_to_refresh"),
                state = pullToRefreshState,
                indicator = {
                    PullToRefreshDefaults.Indicator(
                        state = pullToRefreshState,
                        isRefreshing = isRefreshing,
                        modifier = Modifier.align(Alignment.TopCenter),
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        color = GoldPrimary
                    )
                }
            ) {
                if (filteredChats.isEmpty()) {
                    val emptyType = when {
                        searchQuery.isNotBlank() -> ChatEmptyStateType.SEARCH_NO_RESULTS
                        showArchivedOnly -> ChatEmptyStateType.ARCHIVE_EMPTY
                        showOnlyUnreadFilter -> ChatEmptyStateType.UNREAD_EMPTY
                        selectedTypeFilter != null -> ChatEmptyStateType.TYPE_FILTER_EMPTY
                        selectedGroupFilter != null -> ChatEmptyStateType.TYPE_FILTER_EMPTY
                        else -> ChatEmptyStateType.INBOX_EMPTY
                    }

                    val filterTitle = selectedTypeFilter?.let {
                        if (it == UserType.GOLD_TRADER.name) "تجار الذهب" else "الحسابات المالية"
                    } ?: (selectedGroupFilter ?: "")

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        ExecutiveChatEmptyState(
                            emptyType = emptyType,
                            searchQuery = searchQuery,
                            filterTitle = filterTitle,
                            onNewChatClick = onNewChatClick,
                            onRefresh = onRefresh,
                            onResetFilters = {
                                searchQuery = ""
                                selectedTypeFilter = null
                                selectedGroupFilter = null
                                showOnlyUnreadFilter = false
                                showArchivedOnly = false
                            },
                            onOpenAIAccountant = onOpenAIAccountant
                        )
                    }
                } else {
                    val chatListState = rememberLazyListState()
                    val isFilteringChats = searchQuery.isNotBlank() || selectedTypeFilter != null || selectedGroupFilter != null || showOnlyUnreadFilter
                    var visibleChatsLimit by remember { mutableIntStateOf(25) }
                    val displayedChats = remember(filteredChats, visibleChatsLimit, isFilteringChats) {
                        if (isFilteringChats) filteredChats else filteredChats.take(visibleChatsLimit)
                    }

                    LaunchedEffect(chatListState, displayedChats.size, filteredChats.size, isFilteringChats) {
                        if (isFilteringChats || displayedChats.size >= filteredChats.size) return@LaunchedEffect
                        snapshotFlow {
                            val lastVisible = chatListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                            lastVisible >= (displayedChats.size - 5)
                        }
                            .distinctUntilChanged()
                            .collect { nearEnd ->
                                if (nearEnd && visibleChatsLimit < filteredChats.size) {
                                    visibleChatsLimit += 25
                                }
                            }
                    }

                    LazyColumn(
                        state = chatListState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(displayedChats, key = { it.id }) { chat ->
                            SwipeableChatItemRow(
                                chat = chat,
                                currentUserId = currentUser?.id,
                                onClick = { onChatClick(chat.id) },
                                onLongClickWithAnchor = { topPx, heightPx ->
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    anchoredChatMenu = AnchoredChatMenuState(
                                        chat = chat,
                                        anchorTopPx = topPx,
                                        anchorHeightPx = heightPx
                                    )
                                },
                                onSwipeArchive = {
                                    // Commit any previous pending delete immediately before starting new undo window
                                    val prev = pendingUndoAction
                                    if (prev != null && prev.isDelete) {
                                        pendingDeleteChatIds = pendingDeleteChatIds - prev.chatId
                                        onDeleteChat(prev.chatId)
                                    }
                                    val wasArchived = chat.isArchived
                                    onArchiveChat(chat.id, !wasArchived)
                                    pendingUndoAction = PendingChatUndoAction(
                                        chatId = chat.id,
                                        label = if (wasArchived) "تم إلغاء الأرشفة" else "تمت أرشفة المحادثة",
                                        isDelete = false,
                                        previousArchived = wasArchived
                                    )
                                },
                                onSwipeDelete = {
                                    chatToDeleteConfirm = chat
                                }
                            )
                        }

                        if (!isFilteringChats && displayedChats.size < filteredChats.size) {
                            item(key = "chat_list_load_more") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Surface(
                                        onClick = { visibleChatsLimit += 25 },
                                        shape = RoundedCornerShape(16.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                                    ) {
                                        Text(
                                            text = "عرض محادثات أقدم (${filteredChats.size - displayedChats.size})",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = GoldPrimary,
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 1-Second Floating Undo Pill
        AnimatedVisibility(
            visible = pendingUndoAction != null,
            enter = fadeIn(tween(150)) + slideInVertically(initialOffsetY = { it / 2 }),
            exit = fadeOut(tween(150)) + slideOutVertically(targetOffsetY = { it / 2 }),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 22.dp)
                .testTag("chat_undo_pill")
        ) {
            val action = pendingUndoAction
            if (action != null) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shadowElevation = 8.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = action.label,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                            onClick = {
                                if (action.isDelete) {
                                    pendingDeleteChatIds = pendingDeleteChatIds - action.chatId
                                } else {
                                    onArchiveChat(action.chatId, action.previousArchived)
                                }
                                pendingUndoAction = null
                            },
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = "تراجع",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // Floating Action Button (+)
        AnimatedVisibility(
            visible = isBottomBarVisible,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
        ) {
            FloatingActionButton(
                onClick = { showPlusMenuBottomSheet = true },
                containerColor = adaptiveGoldPrimary,
                contentColor = adaptiveOnPrimary,
                shape = CircleShape,
                modifier = Modifier.size(50.dp),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "إضافة محادثة جديدة", modifier = Modifier.size(23.dp))
            }
        }
    }

    // Anchored Smart Popover Menu on Long Press (opens downward in top half, upward in bottom half)
    if (anchoredChatMenu != null) {
        val menuState = anchoredChatMenu!!
        val chat = menuState.chat
        val screenCenterY = rootTopYInWindow + (rootHeightPx / 2f)
        val rowCenterY = menuState.anchorTopPx + (menuState.anchorHeightPx / 2f)
        val openUpward = rowCenterY >= screenCenterY

        AnchoredChatActionPopover(
            chat = chat,
            anchorTopPx = menuState.anchorTopPx,
            anchorHeightPx = menuState.anchorHeightPx,
            openUpward = openUpward,
            onDismiss = { anchoredChatMenu = null },
            onAssignToWorkGroupClick = {
                anchoredChatMenu = null
                chatToAssignWorkGroup = chat
            },
            onArchiveClick = {
                val wasArchived = chat.isArchived
                onArchiveChat(chat.id, !wasArchived)
                pendingUndoAction = PendingChatUndoAction(
                    chatId = chat.id,
                    label = if (wasArchived) "تم إلغاء الأرشفة" else "تمت أرشفة المحادثة",
                    isDelete = false,
                    previousArchived = wasArchived
                )
            },
            onDeleteClick = {
                chatToDeleteConfirm = chat
            },
            onTogglePinClick = {
                onTogglePinChat(chat.id, !chat.isPinned)
            },
            onMarkAsReadClick = {
                onMarkChatAsRead(chat.id)
            }
        )
    }

    if (chatToAssignWorkGroup != null) {
        val targetChat = chatToAssignWorkGroup!!
        AssignContactToProjectWorkGroupDialog(
            chat = targetChat,
            workGroups = workGroups,
            projects = projects,
            onDismiss = { chatToAssignWorkGroup = null },
            onConfirmAssign = { workGroup, project, roleLabel, projectRole, canPost, canEdit, canExport, canMessage, isAdmin ->
                onAssignToProjectWorkGroup(
                    targetChat,
                    workGroup,
                    project,
                    roleLabel,
                    projectRole,
                    canPost,
                    canEdit,
                    canExport,
                    canMessage,
                    isAdmin
                )
                chatToAssignWorkGroup = null
            }
        )
    }

    // Security Verification Dialog for Chat Deletion (PIN / Biometric / Device Lock)
    if (chatToDeleteConfirm != null) {
        val chat = chatToDeleteConfirm!!
        val expectedPin = currentUser?.appLockPin?.takeIf { it.isNotBlank() }
            ?: context.getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)
                .getString("repeat_user_pin", "1234")
            ?: "1234"
        SecurityVerificationDialog(
            title = "تأكيد حذف المحادثة",
            subtitle = "حذف محادثة ${chat.contactName}",
            expectedPin = expectedPin,
            onDismiss = { chatToDeleteConfirm = null },
            onVerified = {
                val prev = pendingUndoAction
                if (prev != null && prev.isDelete) {
                    pendingDeleteChatIds = pendingDeleteChatIds - prev.chatId
                    onDeleteChat(prev.chatId)
                }
                chatToDeleteConfirm = null
                pendingDeleteChatIds = pendingDeleteChatIds + chat.id
                pendingUndoAction = PendingChatUndoAction(
                    chatId = chat.id,
                    label = "تم حذف المحادثة",
                    isDelete = true,
                    previousArchived = chat.isArchived
                )
            }
        )
    }

    // Discreet Block Confirmation Dialog
    if (chatToBlockConfirm != null) {
        val targetChat = chatToBlockConfirm!!
        val isAlreadyBlocked = BlockManager.isUserBlocked(context, targetChat.contactId)
        AlertDialog(
            onDismissRequest = { chatToBlockConfirm = null },
            icon = {
                Icon(
                    imageVector = if (isAlreadyBlocked) Icons.Default.CheckCircle else Icons.Default.Block,
                    contentDescription = null,
                    tint = if (isAlreadyBlocked) Color(0xFF10B981) else Color(0xFFDC2626),
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = if (isAlreadyBlocked) "إلغاء حظر المحادثة" else "حظر المحادثة والمستخدم",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isAlreadyBlocked) {
                            "هل تريد إلغاء حظر ${targetChat.contactName} واستعادة إمكانية التواصل بشكل طبيعي؟"
                        } else {
                            "هل تريد حظر ${targetChat.contactName}؟"
                        },
                        fontSize = 13.5.sp
                    )
                    if (!isAlreadyBlocked) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "ميزة الخصوصية الذكية: لن تظهر للطرف الآخر أي علامة تدل على الحظر مطلقاً، وسيتم حجب تحديثات ملفك الشخصي (مثل الصورة الشخصية والحالة) عنه تلقائياً.",
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isAlreadyBlocked) {
                            BlockManager.unblockUser(context, targetChat.contactId)
                            android.widget.Toast.makeText(context, "تم إلغاء حظر المحادثة", android.widget.Toast.LENGTH_SHORT).show()
                        } else {
                            BlockManager.blockUser(context, targetChat.contactId)
                            android.widget.Toast.makeText(context, "تم حظر المستخدم وحجب تحديثاتك عنه", android.widget.Toast.LENGTH_LONG).show()
                        }
                        chatToBlockConfirm = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isAlreadyBlocked) Color(0xFF10B981) else Color(0xFFDC2626),
                        contentColor = Color.White
                    )
                ) {
                    Text(if (isAlreadyBlocked) "إلغاء الحظر" else "تأكيد الحظر", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { chatToBlockConfirm = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Hidden Profile Unlocked Dialog
    if (unlockedProfileSuccess != null) {
        val profile = unlockedProfileSuccess!!
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { unlockedProfileSuccess = null },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shadowElevation = 12.dp,
                border = BorderStroke(
                    1.dp,
                    GoldPrimary.copy(alpha = 0.4f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(GoldPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LockOpen,
                            contentDescription = null,
                            tint = GoldPrimary,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "تم فتح الخزينة المشفرة",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${profile.name} - ${profile.companyName}",
                            fontSize = 13.sp,
                            color = GoldPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = profile.roleTitle,
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsOff,
                                contentDescription = null,
                                tint = StatusGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "تم التحويل إلى وضع العمل المحاسبي. الإشعارات الخارجية معطلة لضمان الخصوصية.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { unlockedProfileSuccess = null },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("المحادثات", fontSize = 13.sp)
                        }

                        Button(
                            onClick = {
                                unlockedProfileSuccess = null
                                onOpenCompanyAccounting()
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GoldPrimary,
                                contentColor = MaterialTheme.colorScheme.background
                            ),
                            modifier = Modifier.weight(1.5f)
                        ) {
                            Text("دفاتر المحاسبة", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }

    // Compact Floating Card Menu when clicking (+) FAB
    if (showPlusMenuBottomSheet) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showPlusMenuBottomSheet = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) { showPlusMenuBottomSheet = false }
                    .padding(horizontal = 20.dp, vertical = 92.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    shadowElevation = 12.dp,
                    border = BorderStroke(1.dp, adaptiveGoldPrimary.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .widthIn(min = 190.dp, max = 230.dp)
                        .clickable(enabled = false) {}
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // 1. محادثة جديدة
                        Surface(
                            onClick = {
                                showPlusMenuBottomSheet = false
                                onNewChatClick()
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = Color.Transparent,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Chat,
                                    contentDescription = "محادثة جديدة",
                                    tint = adaptiveGoldPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "محادثة جديدة",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                            }
                        }

                        // 2. مجلد جديد
                        Surface(
                            onClick = {
                                showPlusMenuBottomSheet = false
                                showCreateGroupDialog = true
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = Color.Transparent,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CreateNewFolder,
                                    contentDescription = "مجلد جديد",
                                    tint = adaptiveGoldPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "مجلد جديد",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                            }
                        }

                        // 2b. مجموعة عمل جديدة (مجموعة محادثة + أعضاء ومهام)
                        Surface(
                            onClick = {
                                showPlusMenuBottomSheet = false
                                showCreateWorkGroupDialog = true
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = Color.Transparent,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("chat_plus_new_workgroup_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Groups,
                                    contentDescription = "مجموعة عمل جديدة",
                                    tint = Color(0xFF3B82F6),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "مجموعة عمل جديدة",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                            }
                        }

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        // 3. المساعد الذكي
                        Surface(
                            onClick = {
                                showPlusMenuBottomSheet = false
                                onOpenAIAccountant()
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = adaptiveGoldPrimary.copy(alpha = 0.12f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "المساعد الذكي",
                                    tint = adaptiveGoldPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "المساعد الذكي",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = adaptiveGoldPrimary,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Create Folder Dialog (تجميع محادثات في مجلد عرض دون تحويلها لمجموعة)
    if (showCreateGroupDialog) {
        CreateGroupFolderDialog(
            chats = chats,
            onDismiss = { showCreateGroupDialog = false },
            onCreateGroup = { groupName, selectedIds ->
                onCreateGroupFolder(groupName, selectedIds)
                selectedGroupFilter = groupName
            }
        )
    }

    // Create Standalone WorkGroup Dialog (مجموعة عمل حقيقية مع محادثة وأعضاء ومهام)
    if (showCreateWorkGroupDialog) {
        CreateStandaloneWorkGroupDialog(
            contacts = contacts,
            onDismiss = { showCreateWorkGroupDialog = false },
            onCreateWorkGroup = { name, desc, selectedContacts, disableDMs ->
                showCreateWorkGroupDialog = false
                onCreateStandaloneWorkGroup(name, desc, selectedContacts, disableDMs) { newChatId ->
                    onChatClick(newChatId)
                }
            }
        )
    }

    // Edit Custom Folder Dialog (Rename, edit members, or delete)
    if (editingCustomFolderName != null) {
        val targetName = editingCustomFolderName!!
        CreateGroupFolderDialog(
            chats = chats,
            initialGroupName = targetName,
            initialSelectedChatIds = chats.filter { it.groupName == targetName }.map { it.id },
            onDismiss = { editingCustomFolderName = null },
            onCreateGroup = { newName, selectedIds ->
                topGroupConfigs = ChatFolderPrefs.renameCustomFolderPreservingOrder(context, targetName, newName)
                onRenameGroupFolder(targetName, newName, selectedIds)
                if (selectedGroupFilter == targetName) {
                    selectedGroupFilter = newName
                }
            },
            onDeleteGroup = {
                topGroupConfigs = ChatFolderPrefs.deleteTopGroup(context, ChatFolderPrefs.customIdForName(targetName))
                onRemoveGroupFolder(targetName)
                if (selectedGroupFilter == targetName) {
                    selectedGroupFilter = null
                }
            }
        )
    }

    // Edit Built-In Top Group Dialog (Rename or Delete/Hide)
    if (editingTopGroupTarget != null) {
        val target = editingTopGroupTarget!!
        EditBuiltInTopGroupDialog(
            group = target,
            onDismiss = { editingTopGroupTarget = null },
            onSaveName = { newName ->
                topGroupConfigs = ChatFolderPrefs.renameTopGroup(context, target.id, newName)
                editingTopGroupTarget = null
            },
            onDeleteGroup = {
                topGroupConfigs = ChatFolderPrefs.deleteTopGroup(context, target.id)
                when (target.id) {
                    ChatFolderPrefs.KEY_UNREAD -> showOnlyUnreadFilter = false
                    ChatFolderPrefs.KEY_ARCHIVED -> showArchivedOnly = false
                    ChatFolderPrefs.KEY_GOLD_TRADER -> if (selectedTypeFilter == UserType.GOLD_TRADER.name) selectedTypeFilter = null
                    ChatFolderPrefs.KEY_BUSINESSMAN -> if (selectedTypeFilter == UserType.BUSINESSMAN.name) selectedTypeFilter = null
                }
                editingTopGroupTarget = null
            }
        )
    }

    // Manage All Top Groups Dialog (Unified ordered list: create, edit, delete, reorder)
    if (showManageTopGroupsDialog) {
        ManageAllTopGroupsDialog(
            orderedGroups = topGroupConfigs,
            onDismiss = { showManageTopGroupsDialog = false },
            onMoveGroup = { groupId, delta ->
                topGroupConfigs = ChatFolderPrefs.moveTopGroup(context, groupId, delta)
            },
            onEditGroup = { cfg ->
                showManageTopGroupsDialog = false
                if (cfg.isBuiltIn) {
                    editingTopGroupTarget = cfg
                } else {
                    editingCustomFolderName = cfg.filterKey
                }
            },
            onDeleteGroup = { cfg ->
                topGroupConfigs = ChatFolderPrefs.deleteTopGroup(context, cfg.id)
                if (cfg.isBuiltIn) {
                    when (cfg.id) {
                        ChatFolderPrefs.KEY_UNREAD -> showOnlyUnreadFilter = false
                        ChatFolderPrefs.KEY_ARCHIVED -> showArchivedOnly = false
                        ChatFolderPrefs.KEY_GOLD_TRADER -> if (selectedTypeFilter == UserType.GOLD_TRADER.name) selectedTypeFilter = null
                        ChatFolderPrefs.KEY_BUSINESSMAN -> if (selectedTypeFilter == UserType.BUSINESSMAN.name) selectedTypeFilter = null
                    }
                } else {
                    onRemoveGroupFolder(cfg.filterKey)
                    if (selectedGroupFilter == cfg.filterKey) selectedGroupFilter = null
                }
            },
            onRestoreBuiltInDefaults = {
                topGroupConfigs = ChatFolderPrefs.restoreBuiltInDefaults(context)
            },
            onAddNewGroup = {
                showManageTopGroupsDialog = false
                showCreateGroupDialog = true
            }
        )
    }

    if (showCommandCenter) {
        CommandCenterDialog(
            chats = chats,
            goldPrices = goldPrices,
            onDismiss = { showCommandCenter = false },
            onSelectChat = onChatClick,
            onNewChatClick = onNewChatClick,
            onGoldDealClick = onOpenGoldDealClick,
            onNewProjectClick = onOpenNewProjectClick,
            onSmartAccountantClick = onOpenAIAccountant
        )
    }

    if (featureGatedDecision != null) {
        FeatureGatedDialog(
            decision = featureGatedDecision!!,
            onDismiss = { featureGatedDecision = null },
            onOpenSubscriptions = {
                featureGatedDecision = null
                onOpenSubscriptions()
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TopEditableGroupChip(
    label: String,
    isSelected: Boolean,
    isDark: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val bgColor = when {
        isSelected -> adaptiveGoldPrimary
        isDark -> GoldDark.copy(alpha = 0.28f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f)
    }
    val contentColor = when {
        isSelected -> adaptiveOnPrimary
        isDark -> GoldLight
        else -> MaterialTheme.colorScheme.onSurface
    }
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = bgColor,
        border = BorderStroke(
            width = 0.8.dp,
            color = if (isSelected) adaptiveGoldPrimary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
        ),
        modifier = Modifier
            .heightIn(min = 34.dp)
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(14.dp)
                )
            }
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = contentColor,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun EditBuiltInTopGroupDialog(
    group: TopChatGroupConfig,
    onDismiss: () -> Unit,
    onSaveName: (String) -> Unit,
    onDeleteGroup: () -> Unit
) {
    var editedName by remember { mutableStateOf(group.label) }
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.42f))
                .navigationBarsPadding()
                .imePadding()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("تعديل المجموعة", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "إغلاق")
                        }
                    }

                    OutlinedTextField(
                        value = editedName,
                        onValueChange = { editedName = it },
                        label = { Text("اسم المجموعة") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDeleteGroup,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusRed),
                            border = BorderStroke(1.dp, StatusRed.copy(alpha = 0.45f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("حذف", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { if (editedName.isNotBlank()) onSaveName(editedName.trim()) },
                            enabled = editedName.isNotBlank(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = MaterialTheme.colorScheme.onPrimary),
                            modifier = Modifier
                                .weight(1.3f)
                                .height(48.dp)
                        ) {
                            Text("حفظ التعديل", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ManageAllTopGroupsDialog(
    orderedGroups: List<TopChatGroupConfig>,
    onDismiss: () -> Unit,
    onMoveGroup: (groupId: String, delta: Int) -> Unit,
    onEditGroup: (TopChatGroupConfig) -> Unit,
    onDeleteGroup: (TopChatGroupConfig) -> Unit,
    onRestoreBuiltInDefaults: () -> Unit,
    onAddNewGroup: () -> Unit
) {
    val visibleGroups = remember(orderedGroups) { orderedGroups.filter { !it.isHidden } }
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
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp)
                    .heightIn(max = 580.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("إدارة وترتيب المجموعات", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "إغلاق")
                        }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(visibleGroups.size, key = { idx -> visibleGroups[idx].id }) { idx ->
                            val cfg = visibleGroups[idx]
                            val canMoveUp = idx > 0
                            val canMoveDown = idx < visibleGroups.lastIndex
                            val isCustom = !cfg.isBuiltIn
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isCustom) GoldPrimary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
                                border = if (isCustom) BorderStroke(0.8.dp, GoldPrimary.copy(alpha = 0.35f)) else null,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Up / Down ordering controls
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(end = 6.dp)
                                    ) {
                                        IconButton(
                                            onClick = { onMoveGroup(cfg.id, -1) },
                                            enabled = canMoveUp,
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.KeyboardArrowUp,
                                                contentDescription = "تحريك لأعلى",
                                                tint = if (canMoveUp) GoldPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.28f),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = { onMoveGroup(cfg.id, 1) },
                                            enabled = canMoveDown,
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.KeyboardArrowDown,
                                                contentDescription = "تحريك لأسفل",
                                                tint = if (canMoveDown) GoldPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.28f),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    Text(
                                        text = cfg.label,
                                        fontSize = 13.5.sp,
                                        fontWeight = if (isCustom) FontWeight.Bold else FontWeight.SemiBold,
                                        color = if (isCustom) GoldPrimary else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    IconButton(onClick = { onEditGroup(cfg) }, modifier = Modifier.size(42.dp)) {
                                        Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = GoldPrimary, modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(onClick = { onDeleteGroup(cfg) }, modifier = Modifier.size(42.dp)) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "حذف", tint = StatusRed, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (orderedGroups.any { it.isHidden }) {
                            OutlinedButton(
                                onClick = onRestoreBuiltInDefaults,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Text("استعادة المحذوفة", fontSize = 12.sp)
                            }
                        }
                        Button(
                            onClick = onAddNewGroup,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = MaterialTheme.colorScheme.onPrimary),
                            modifier = Modifier
                                .weight(1.2f)
                                .height(48.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("إنشاء مجموعة جديدة", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CreateGroupFolderDialog(
    chats: List<ChatEntity>,
    initialGroupName: String? = null,
    initialSelectedChatIds: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onCreateGroup: (groupName: String, selectedChatIds: List<String>) -> Unit,
    onDeleteGroup: (() -> Unit)? = null
) {
    val isEditMode = !initialGroupName.isNullOrBlank()
    var groupName by remember(initialGroupName) { mutableStateOf(initialGroupName.orEmpty()) }
    val selectedChatIds = remember(initialGroupName) {
        mutableStateListOf<String>().apply { addAll(initialSelectedChatIds) }
    }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
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
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 580.dp),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shadowElevation = 12.dp,
                border = BorderStroke(
                    1.dp,
                    GoldPrimary.copy(alpha = 0.4f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
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
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(GoldPrimary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FolderSpecial,
                                    contentDescription = null,
                                    tint = GoldPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Text(
                                text = if (isEditMode) "تعديل المجلد" else "مجلد جديد",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isEditMode && onDeleteGroup != null) {
                                IconButton(
                                    onClick = {
                                        onDeleteGroup()
                                        onDismiss()
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "حذف المجلد",
                                        tint = StatusRed,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "إغلاق",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = groupName,
                        onValueChange = { groupName = it },
                        label = { Text("اسم المجلد", fontSize = 12.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .heightIn(max = 260.dp)
                            .border(
                                0.8.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(chats, key = { it.id }) { chat ->
                            val isChecked = selectedChatIds.contains(chat.id)
                            Surface(
                                onClick = {
                                    if (isChecked) selectedChatIds.remove(chat.id)
                                    else selectedChatIds.add(chat.id)
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isChecked) GoldPrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                                border = if (isChecked) BorderStroke(0.8.dp, GoldPrimary.copy(alpha = 0.5f)) else null,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = {
                                            if (it) selectedChatIds.add(chat.id)
                                            else selectedChatIds.remove(chat.id)
                                        },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = GoldPrimary,
                                            checkmarkColor = MaterialTheme.colorScheme.background
                                        ),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = chat.contactName,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

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
                            Text("إلغاء", fontSize = 13.sp)
                        }

                        Button(
                            onClick = {
                                if (groupName.isNotBlank() && selectedChatIds.isNotEmpty()) {
                                    onCreateGroup(groupName.trim(), selectedChatIds.toList())
                                    onDismiss()
                                }
                            },
                            enabled = groupName.isNotBlank() && selectedChatIds.isNotEmpty(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GoldPrimary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                        ) {
                            Text("حفظ", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SpatialOverviewCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    onClick: () -> Unit
) {
    SpatialSurface(
        elevation = SpatialElevation.Level2,
        shape = RoundedCornerShape(16.dp),
        backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        borderColor = accentColor.copy(alpha = 0.3f),
        onClick = onClick,
        modifier = Modifier.width(160.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = value,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = accentColor
            )
        }
    }
}

@Composable
fun SwipeableChatItemRow(
    chat: ChatEntity,
    currentUserId: String? = null,
    onClick: () -> Unit,
    onLongClickWithAnchor: (anchorTopPx: Float, anchorHeightPx: Float) -> Unit,
    onSwipeArchive: () -> Unit,
    onSwipeDelete: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val offsetX = remember { androidx.compose.animation.core.Animatable(0f) }
    val maxSwipeDp = 112.dp
    val density = LocalDensity.current
    val maxSwipePx = with(density) { maxSwipeDp.toPx() }
    val thresholdPx = maxSwipePx * 0.65f
    var triggeredHaptic by remember { mutableStateOf(false) }
    var rowTopYPx by remember { mutableFloatStateOf(0f) }
    var rowHeightPx by remember { mutableFloatStateOf(0f) }

    val colors = MaterialTheme.colorScheme
    val isSwipingRight = offsetX.value > 4f
    val isSwipingLeft = offsetX.value < -4f
    val progress = (abs(offsetX.value) / thresholdPx).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .onGloballyPositioned { coords ->
                rowTopYPx = coords.positionInWindow().y
                rowHeightPx = coords.size.height.toFloat()
            }
    ) {
        // Directional Reveal Backdrop (Forced LTR so physical left/right match finger drag)
        if (isSwipingRight || isSwipingLeft) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            if (isSwipingRight) {
                                colors.primary.copy(alpha = 0.16f + 0.14f * progress)
                            } else {
                                colors.error.copy(alpha = 0.16f + 0.16f * progress)
                            }
                        )
                        .padding(horizontal = 20.dp),
                    contentAlignment = if (isSwipingRight) Alignment.CenterStart else Alignment.CenterEnd
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.graphicsLayer {
                            val s = 0.85f + 0.15f * progress
                            scaleX = s
                            scaleY = s
                            alpha = 0.45f + 0.55f * progress
                        }
                    ) {
                        if (isSwipingRight) {
                            Icon(
                                imageVector = if (chat.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                                contentDescription = if (chat.isArchived) "إلغاء الأرشفة" else "أرشفة",
                                tint = colors.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = if (chat.isArchived) "إلغاء الأرشفة" else "أرشفة",
                                color = colors.primary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Text(
                                text = "حذف",
                                color = colors.error,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                imageVector = Icons.Default.DeleteForever,
                                contentDescription = "حذف",
                                tint = colors.error,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }

        // Foreground Chat Card moving in exact physical drag direction
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { translationX = offsetX.value }
                .pointerInput(chat.id) {
                    detectHorizontalDragGestures(
                        onDragStart = {
                            triggeredHaptic = false
                        },
                        onDragCancel = {
                            coroutineScope.launch {
                                offsetX.animateTo(0f, tween(180))
                            }
                        },
                        onDragEnd = {
                            coroutineScope.launch {
                                val currentVal = offsetX.value
                                when {
                                    currentVal >= thresholdPx -> {
                                        offsetX.animateTo(0f, spring(stiffness = Spring.StiffnessMedium))
                                        onSwipeArchive()
                                    }
                                    currentVal <= -thresholdPx -> {
                                        offsetX.animateTo(0f, spring(stiffness = Spring.StiffnessMedium))
                                        onSwipeDelete()
                                    }
                                    else -> {
                                        offsetX.animateTo(0f, spring(stiffness = Spring.StiffnessMedium))
                                    }
                                }
                            }
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            coroutineScope.launch {
                                val newVal = (offsetX.value + dragAmount).coerceIn(-maxSwipePx * 1.15f, maxSwipePx * 1.15f)
                                offsetX.snapTo(newVal)
                                val crossed = abs(newVal) >= thresholdPx
                                if (crossed && !triggeredHaptic) {
                                    triggeredHaptic = true
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                } else if (!crossed) {
                                    triggeredHaptic = false
                                }
                            }
                        }
                    )
                }
        ) {
            SpatialChatItemCard(
                chat = chat,
                currentUserId = currentUserId,
                onClick = onClick,
                onLongClick = { onLongClickWithAnchor(rowTopYPx, rowHeightPx) }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SpatialChatItemCard(
    chat: ChatEntity,
    currentUserId: String? = null,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val hasUnread = chat.unreadCount > 0

    val cardBg = if (isDark) {
        if (hasUnread) GoldPrimary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    val cardBorder = if (isDark) {
        when {
            hasUnread -> GoldPrimary.copy(alpha = 0.75f)
            chat.isPinned -> GoldPrimary.copy(alpha = 0.55f)
            chat.contactType == UserType.GOLD_TRADER.name -> GoldAccent.copy(alpha = 0.35f)
            else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.18f)
        }
    } else {
        when {
            hasUnread -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            chat.isPinned -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        }
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = cardBg,
        border = BorderStroke(
            width = if (hasUnread) 1.5.dp else if (chat.isPinned) 1.2.dp else 1.dp,
            color = cardBorder
        ),
        shadowElevation = if (isDark) {
            if (hasUnread) 3.dp else 1.dp
        } else {
            if (hasUnread) 2.dp else 1.dp
        },
        modifier = Modifier
            .fillMaxWidth()
            .testTag("chat_item_${chat.id}")
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar with unread indicator badge & gold border
            Box(contentAlignment = Alignment.TopStart) {
                OozoxAvatarImage(
                    model = null,
                    name = chat.contactName,
                    size = 48.dp,
                    isGroup = chat.isGroup,
                    isTrader = chat.contactType == UserType.GOLD_TRADER.name,
                    contentDescription = chat.contactName
                )

                if (hasUnread) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(GoldPrimary)
                            .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                            .testTag("unread_dot_${chat.id}")
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Row 1: Contact Name + Verified/Pin/Role Badge + Timestamp
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = chat.contactName,
                            fontSize = 15.sp,
                            fontWeight = if (hasUnread) FontWeight.ExtraBold else FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        if (chat.contactVerified) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = "موثق",
                                tint = GoldAccent,
                                modifier = Modifier.size(15.dp)
                            )
                        }

                        if (chat.isPinned) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = "محادثة مثبتة",
                                tint = GoldPrimary,
                                modifier = Modifier.size(13.dp)
                            )
                        }

                        if (chat.isArchived) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Archive,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = "مؤرشفة",
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        // Contextual Badges with compact styling
                        if (chat.isGroup) {
                            Spacer(modifier = Modifier.width(5.dp))
                            Surface(
                                color = Color(0xFF3B82F6).copy(alpha = 0.18f),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(0.8.dp, Color(0xFF3B82F6).copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "مجموعة",
                                    fontSize = 9.5.sp,
                                    color = Color(0xFF60A5FA),
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                                )
                            }
                        } else if (!chat.groupName.isNullOrBlank()) {
                            Spacer(modifier = Modifier.width(5.dp))
                            Surface(
                                color = GoldPrimary.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(0.8.dp, GoldPrimary.copy(alpha = 0.35f))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Folder,
                                        contentDescription = null,
                                        tint = GoldPrimary,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Text(
                                        text = "${chat.groupName}",
                                        fontSize = 9.5.sp,
                                        color = GoldPrimary,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }
                            }
                        } else if (!chat.contactType.isNullOrBlank()) {
                            val displayRole = when (chat.contactType) {
                                UserType.GOLD_TRADER.name -> "تاجر ذهب"
                                UserType.BUSINESSMAN.name -> "رجل أعمال"
                                else -> chat.contactType
                            }
                            Spacer(modifier = Modifier.width(5.dp))
                            Surface(
                                color = if (isDark) GoldPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(0.8.dp, GoldPrimary.copy(alpha = 0.35f))
                            ) {
                                Text(
                                    text = displayRole,
                                    fontSize = 9.5.sp,
                                    color = if (isDark) GoldLight else adaptiveGoldPrimary,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                                )
                            }
                        }
                    }

                    if (chat.lastMessageTime != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = GoldUtils.formatShortTime(chat.lastMessageTime),
                            fontSize = 11.sp,
                            fontWeight = if (hasUnread) FontWeight.Bold else FontWeight.Medium,
                            color = if (hasUnread) GoldPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                            maxLines = 1
                        )
                    }
                }

                // Row 2: Receipt + Last Message Preview + Unread Count Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentUserId != null && chat.lastMessageSenderId == currentUserId) {
                        when (chat.lastMessageStatus) {
                            MessageStatus.READ.name -> {
                                Icon(
                                    imageVector = Icons.Default.DoneAll,
                                    contentDescription = "تمت القراءة",
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier
                                        .size(15.dp)
                                        .testTag("chat_list_receipt_read_${chat.id}")
                                )
                            }
                            MessageStatus.DELIVERED.name -> {
                                Icon(
                                    imageVector = Icons.Default.DoneAll,
                                    contentDescription = "تم التسليم",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier
                                        .size(15.dp)
                                        .testTag("chat_list_receipt_delivered_${chat.id}")
                                )
                            }
                            else -> {
                                Icon(
                                    imageVector = Icons.Default.Done,
                                    contentDescription = "تم الإرسال",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                                    modifier = Modifier
                                        .size(14.dp)
                                        .testTag("chat_list_receipt_sent_${chat.id}")
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    val sanitizedPreview = remember(chat.lastMessage) {
                        ContentSafetyEngine.sanitizePreview(chat.lastMessage)
                    }
                    val isMissedCall = sanitizedPreview.contains("مكالمة فائتة")

                    Text(
                        text = if (sanitizedPreview.isNotBlank()) sanitizedPreview else "لا توجد رسائل بعد",
                        fontSize = 12.5.sp,
                        lineHeight = 17.sp,
                        fontWeight = if (hasUnread) FontWeight.SemiBold else FontWeight.Normal,
                        color = when {
                            isMissedCall -> Color(0xFFEF4444)
                            sanitizedPreview.startsWith("️") -> StatusOrange
                            hasUnread -> MaterialTheme.colorScheme.onSurface
                            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (hasUnread) {
                        Spacer(modifier = Modifier.width(8.dp))
                        val chatBadgeSize = if (chat.unreadCount > 99) 24.dp else 20.dp
                        Surface(
                            color = adaptiveGoldPrimary,
                            shape = CircleShape,
                            shadowElevation = 2.dp,
                            modifier = Modifier
                                .size(chatBadgeSize)
                                .testTag("unread_count_badge_${chat.id}")
                                .testTag("unread_count_badge")
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Text(
                                    text = if (chat.unreadCount > 99) "99+" else chat.unreadCount.toString(),
                                    fontSize = if (chat.unreadCount > 99) 8.5.sp else 10.5.sp,
                                    lineHeight = 10.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = adaptiveOnPrimary,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    style = LocalTextStyle.current.copy(
                                        platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false)
                                    )
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
private fun AnchoredChatActionPopover(
    chat: ChatEntity,
    anchorTopPx: Float,
    anchorHeightPx: Float,
    openUpward: Boolean,
    onDismiss: () -> Unit,
    onAssignToWorkGroupClick: () -> Unit,
    onArchiveClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onTogglePinClick: () -> Unit,
    onMarkAsReadClick: () -> Unit
) {
    val density = LocalDensity.current
    var menuHeightPx by remember { mutableIntStateOf(0) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.24f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        ) {
            val containerHeightPx = with(density) { maxHeight.toPx() }
            val safeTopPx = with(density) { 48.dp.toPx() }
            val safeBottomPx = containerHeightPx - with(density) { 40.dp.toPx() }
            val gapPx = with(density) { 6.dp.toPx() }

            val rawY = if (openUpward) {
                anchorTopPx - gapPx - menuHeightPx
            } else {
                anchorTopPx + anchorHeightPx + gapPx
            }
            val clampedY = rawY
                .coerceIn(safeTopPx, (safeBottomPx - menuHeightPx).coerceAtLeast(safeTopPx))
                .roundToInt()

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                shadowElevation = 16.dp,
                border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.32f)),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 16.dp, start = 16.dp)
                    .offset { IntOffset(x = 0, y = clampedY) }
                    .widthIn(min = 248.dp, max = 304.dp)
                    .onGloballyPositioned { coords -> menuHeightPx = coords.size.height }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
                    .testTag("anchored_chat_popover_menu")
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Compact contact title strip
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OozoxAvatarImage(
                            model = null,
                            name = chat.contactName,
                            size = 32.dp,
                            isGroup = chat.isGroup,
                            isTrader = chat.contactType == UserType.GOLD_TRADER.name,
                            contentDescription = chat.contactName
                        )
                        Text(
                            text = chat.contactName,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

                    // Add Contact to Project WorkGroup
                    PopoverMenuItemRow(
                        icon = Icons.Default.GroupAdd,
                        label = "إضافة إلى مجموعة عمل مشروع",
                        tint = GoldPrimary,
                        highlighted = true,
                        testTag = "assign_contact_workgroup_action",
                        onClick = onAssignToWorkGroupClick
                    )

                    // Floating Bubble
                    PopoverMenuItemRow(
                        icon = Icons.Default.ChatBubble,
                        label = "فتح كفقاعة عائمة",
                        tint = MaterialTheme.colorScheme.primary,
                        testTag = "open_chat_bubble_action",
                        onClick = {
                            com.example.util.MessengerBubbleManager.showBubble(
                                chatId = chat.id,
                                contactName = chat.contactName,
                                contactAvatarUri = null,
                                contactRole = chat.contactType.takeIf { it.isNotBlank() } ?: "جهة اتصال",
                                messageText = "استمرار المحادثة في الفقاعة العائمة",
                                autoExpand = true
                            )
                            onDismiss()
                        }
                    )

                    // Pin / Unpin
                    PopoverMenuItemRow(
                        icon = Icons.Default.PushPin,
                        label = if (chat.isPinned) "إلغاء تثبيت المحادثة" else "تثبيت المحادثة",
                        tint = GoldPrimary,
                        testTag = "pin_chat_action",
                        onClick = {
                            onTogglePinClick()
                            onDismiss()
                        }
                    )

                    // Archive / Unarchive
                    PopoverMenuItemRow(
                        icon = if (chat.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                        label = if (chat.isArchived) "إلغاء الأرشفة" else "أرشفة المحادثة",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        testTag = "archive_chat_action",
                        onClick = {
                            onArchiveClick()
                            onDismiss()
                        }
                    )

                    // Mark as Read
                    if (chat.unreadCount > 0) {
                        PopoverMenuItemRow(
                            icon = Icons.Default.MarkChatRead,
                            label = "تحديد كمقروء",
                            tint = StatusGreen,
                            testTag = "mark_read_chat_action",
                            onClick = {
                                onMarkAsReadClick()
                                onDismiss()
                            }
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                    // Delete Chat
                    PopoverMenuItemRow(
                        icon = Icons.Default.DeleteOutline,
                        label = "حذف المحادثة",
                        tint = MaterialTheme.colorScheme.error,
                        testTag = "delete_chat_action",
                        onClick = {
                            onDeleteClick()
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PopoverMenuItemRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    highlighted: Boolean = false,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = if (highlighted) tint.copy(alpha = 0.1f) else Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                fontSize = 13.5.sp,
                fontWeight = if (highlighted) FontWeight.Bold else FontWeight.Medium,
                color = if (tint == MaterialTheme.colorScheme.error) tint else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun AssignContactToProjectWorkGroupDialog(
    chat: ChatEntity,
    workGroups: List<WorkGroupEntity>,
    projects: List<ProjectEntity>,
    onDismiss: () -> Unit,
    onConfirmAssign: (
        workGroup: WorkGroupEntity,
        project: ProjectEntity?,
        roleLabel: String,
        projectRole: ProjectMemberRole,
        canPostAccounting: Boolean,
        canEditRecords: Boolean,
        canExportReports: Boolean,
        canMessageDirectly: Boolean,
        isGroupAdmin: Boolean
    ) -> Unit
) {
    var selectedGroup by remember(workGroups) { mutableStateOf(workGroups.firstOrNull()) }
    var selectedProject by remember(projects, selectedGroup) {
        mutableStateOf(
            projects.firstOrNull { it.workGroupId != null && it.workGroupId == selectedGroup?.id }
                ?: projects.firstOrNull()
        )
    }
    var selectedProjectRole by remember { mutableStateOf(ProjectMemberRole.EXECUTIVE_MANAGER) }
    var customRoleLabel by remember { mutableStateOf(ProjectMemberRole.EXECUTIVE_MANAGER.label) }
    var canPostAccounting by remember { mutableStateOf(true) }
    var canEditRecords by remember { mutableStateOf(false) }
    var canExportReports by remember { mutableStateOf(false) }
    var canMessageDirectly by remember { mutableStateOf(true) }
    var isGroupAdmin by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 540.dp)
                    .heightIn(max = 640.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "إضافة إلى مجموعة عمل مشروع",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = chat.contactName,
                                fontSize = 12.5.sp,
                                color = GoldPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "إغلاق")
                        }
                    }

                    if (workGroups.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "لا توجد مجموعات عمل منشأة بعد. أنشئ مجموعة عمل من قسم المشاريع أولاً.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    } else {
                        Text("مجموعة العمل", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            workGroups.forEach { wg ->
                                val isSelected = selectedGroup?.id == wg.id
                                Surface(
                                    onClick = {
                                        selectedGroup = wg
                                        projects.firstOrNull { it.workGroupId == wg.id }?.let { linked ->
                                            selectedProject = linked
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) GoldPrimary.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    border = BorderStroke(1.dp, if (isSelected) GoldPrimary else Color.Transparent),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = null,
                                            colors = RadioButtonDefaults.colors(selectedColor = GoldPrimary)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(wg.name, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                                            wg.description?.takeIf { it.isNotBlank() }?.let { desc ->
                                                Text(desc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (projects.isNotEmpty()) {
                            Text("المشروع المرتبط", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                projects.forEach { proj ->
                                    val isProjSelected = selectedProject?.id == proj.id
                                    FilterChip(
                                        selected = isProjSelected,
                                        onClick = { selectedProject = if (isProjSelected) null else proj },
                                        label = { Text(proj.name, fontSize = 12.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = GoldPrimary,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                        )
                                    )
                                }
                            }
                        }

                        Text("الدور الوظيفي", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ProjectMemberRole.entries.forEach { role ->
                                val isRoleSel = selectedProjectRole == role
                                FilterChip(
                                    selected = isRoleSel,
                                    onClick = {
                                        selectedProjectRole = role
                                        customRoleLabel = role.label
                                    },
                                    label = { Text(role.label, fontSize = 11.5.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = GoldPrimary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                )
                            }
                        }

                        OutlinedTextField(
                            value = customRoleLabel,
                            onValueChange = { customRoleLabel = it },
                            label = { Text("المسمى الوظيفي داخل المجموعة") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text("الصلاحيات الممنوحة", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                        PermissionToggleRow("ترحيل القيود والمحاسبة", canPostAccounting) { canPostAccounting = it }
                        PermissionToggleRow("تعديل السجلات والبيانات", canEditRecords) { canEditRecords = it }
                        PermissionToggleRow("تصدير التقارير وكشوف الحساب", canExportReports) { canExportReports = it }
                        PermissionToggleRow("التراسل المباشر مع الأعضاء", canMessageDirectly) { canMessageDirectly = it }
                        PermissionToggleRow("مشرف / مدير بالمجموعة", isGroupAdmin) { isGroupAdmin = it }

                        Spacer(modifier = Modifier.height(4.dp))

                        Button(
                            onClick = {
                                val wg = selectedGroup ?: return@Button
                                onConfirmAssign(
                                    wg,
                                    selectedProject,
                                    customRoleLabel.trim().ifBlank { selectedProjectRole.label },
                                    selectedProjectRole,
                                    canPostAccounting,
                                    canEditRecords,
                                    canExportReports,
                                    canMessageDirectly,
                                    isGroupAdmin
                                )
                            },
                            enabled = selectedGroup != null,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = MaterialTheme.colorScheme.onPrimary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("confirm_assign_workgroup_btn")
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("حفظ وإضافة لمجموعة العمل", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        onClick = { onCheckedChange(!checked) },
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.onPrimary, checkedTrackColor = GoldPrimary)
            )
        }
    }
}

@Composable
fun CreateStandaloneWorkGroupDialog(
    contacts: List<ContactEntity>,
    onDismiss: () -> Unit,
    onCreateWorkGroup: (groupName: String, description: String?, selectedContacts: List<ContactEntity>, disableMemberDMs: Boolean) -> Unit
) {
    var groupName by remember { mutableStateOf("") }
    var groupDesc by remember { mutableStateOf("") }
    var disableMemberDMs by remember { mutableStateOf(false) }
    val selectedContactIds = remember { mutableStateListOf<String>() }

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
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp)
                    .heightIn(max = 640.dp),
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shadowElevation = 12.dp,
                border = BorderStroke(1.dp, Color(0xFF3B82F6).copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
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
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF3B82F6).copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Groups,
                                    contentDescription = null,
                                    tint = Color(0xFF3B82F6),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text("مجموعة عمل جديدة", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Text("إنشاء محادثة جماعية وإضافة الأعضاء", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "إغلاق")
                        }
                    }

                    OutlinedTextField(
                        value = groupName,
                        onValueChange = { groupName = it },
                        label = { Text("اسم مجموعة العمل *", fontSize = 12.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("workgroup_name_input")
                    )

                    OutlinedTextField(
                        value = groupDesc,
                        onValueChange = { groupDesc = it },
                        label = { Text("الوصف (اختياري)", fontSize = 12.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    PermissionToggleRow(
                        title = "منع التراسل الخاص بين الأعضاء",
                        checked = disableMemberDMs,
                        onCheckedChange = { disableMemberDMs = it }
                    )

                    Text(
                        text = "اختر أعضاء الفريق (${selectedContactIds.size} محدد):",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldPrimary
                    )

                    if (contacts.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "سيتم إضافتك كمدير ويمكنك دعوة الأعضاء لاحقاً",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false)
                                .heightIn(max = 220.dp)
                                .border(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                                .padding(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(contacts, key = { it.id }) { contact ->
                                val isSelected = selectedContactIds.contains(contact.id)
                                Surface(
                                    onClick = {
                                        if (isSelected) selectedContactIds.remove(contact.id)
                                        else selectedContactIds.add(contact.id)
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) GoldPrimary.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                                    border = if (isSelected) BorderStroke(0.8.dp, GoldPrimary.copy(alpha = 0.55f)) else null,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = isSelected,
                                            onCheckedChange = {
                                                if (it) selectedContactIds.add(contact.id)
                                                else selectedContactIds.remove(contact.id)
                                            },
                                            colors = CheckboxDefaults.colors(checkedColor = GoldPrimary),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(contact.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                            if (contact.phone.isNotBlank()) {
                                                Text(contact.phone, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

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
                            Text("إلغاء", fontSize = 13.sp)
                        }
                        Button(
                            onClick = {
                                val chosen = contacts.filter { it.id in selectedContactIds }
                                onCreateWorkGroup(groupName.trim(), groupDesc.trim().ifBlank { null }, chosen, disableMemberDMs)
                            },
                            enabled = groupName.isNotBlank(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                            modifier = Modifier
                                .weight(1.4f)
                                .heightIn(min = 48.dp)
                                .testTag("confirm_create_workgroup_btn")
                        ) {
                            Text("إنشاء المجموعة", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DeleteChatConfirmDialog(
    chat: ChatEntity,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    com.example.ui.components.DeleteChatConfirmationDialog(
        chat = chat,
        onDismiss = onDismiss,
        onConfirm = onConfirm
    )
}

@Composable
fun CompactOozoxLogoHeader(modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
    ) {
        // Official OOZOX Infinity Squircle Badge - compact 28dp
        OozoxThumbnailImage(
            model = com.example.R.drawable.oozox_official_clean_icon_1786654687391,
            width = 28.dp,
            height = 28.dp,
            shape = RoundedCornerShape(8.dp),
            contentDescription = "OOZOX Official Logo"
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "OOZOX",
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = OozoxPlatinum,
                letterSpacing = 1.2.sp
            )
            // Real-time live status dot
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(StatusGreen)
            )
        }
    }
}

@Composable
fun OozoxLogoHeader(modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        // Official OOZOX Infinity Squircle Badge
        OozoxThumbnailImage(
            model = com.example.R.drawable.oozox_official_clean_icon_1786654687391,
            width = 36.dp,
            height = 36.dp,
            shape = RoundedCornerShape(10.dp),
            contentDescription = "OOZOX Official Logo"
        )

        Column {
            Text(
                text = "OOZOX",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = OozoxPlatinum,
                letterSpacing = 1.8.sp
            )
            Text(
                text = "oozox.com",
                fontSize = 9.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = OozoxTeal,
                letterSpacing = 0.4.sp
            )
        }
    }
}

