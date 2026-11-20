package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.LocalAppTheme
import com.example.data.model.*
import com.example.ui.theme.*
import com.example.ui.components.AdminDashboardSkeleton
import com.example.util.FcmNotificationManager
import com.example.util.DatabaseIntegrityChecker
import com.example.util.IntegrityReport
import com.example.util.IntegrityStatus
import com.example.util.SmartProductScannerEngine
import com.example.util.SimpleExcelEngine
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    adminConfig: AdminConfigEntity?,
    traderRequests: List<TraderRequestEntity>,
    allUsers: List<UserEntity>,
    subscriptionPlans: List<SubscriptionPlanEntity>,
    subscriptionRequests: List<SubscriptionRequestEntity>,
    promoAnnouncements: List<PromoAnnouncementEntity>,
    goldPrices: GoldPricesModel,
    allTransactions: List<TransactionEntity>,
    allGoldPurchases: List<GoldPurchaseEntity>,
    allProjects: List<ProjectEntity>,
    allInvoices: List<InvoiceEntity> = emptyList(),
    onBackClick: () -> Unit,
    onApproveTrader: (String) -> Unit,
    onRejectTrader: (String) -> Unit,
    onSaveConfig: (
        appName: String?, appLogoUri: String?, adminPin: String?,
        geminiKey: String?, geminiModel: String?, aiPrompt: String?,
        notificationUrl: String?, notificationKey: String?, newsUrl: String?,
        goldUrl: String?, goldKey: String?, intervalSec: Int?, encryption: Boolean,
        voiceSelection: String?, personaPrompt: String?, biometricAuthEnabled: Boolean?
    ) -> Unit,
    onUpdatePrices: (k21: Double) -> Unit,
    onUpdatePlanPriceAndOffer: (planId: String, monthlyPrice: Double, discountPercent: Int, offerTitle: String?, isOfferActive: Boolean) -> Unit,
    onUpdateSubscriptionRequestStatus: (requestId: String, status: String, notes: String?, userId: String?, planId: String?) -> Unit,
    onExtendUserTrial: (userId: String, days: Long) -> Unit,
    onUpdateUserTierDirectly: (userId: String, tierName: String, days: Long) -> Unit,
    onPostPromoAnnouncement: (title: String, content: String, promoCode: String?, discountPercent: Int, targetPlanId: String?) -> Unit,
    onDeletePromoAnnouncement: (id: String) -> Unit,
    onTogglePromoStatus: (List<String>, Boolean) -> Unit = { _, _ -> },
    onDeleteUsers: (List<String>) -> Unit = {},
    onSendBroadcast: ((BroadcastTargetCategory, String, String, Boolean, Boolean, (Int) -> Unit) -> Unit)? = null,
    isLoading: Boolean = false
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var isRefreshing by remember { mutableStateOf(false) }
    var isInitialLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(350)
        isInitialLoading = false
    }

    // App Identity & System Settings States
    var appName by remember(adminConfig) { mutableStateOf(adminConfig?.appName ?: "OOZOX Business") }
    var appLogoUri by remember(adminConfig) { mutableStateOf(adminConfig?.appLogoUri ?: "") }
    var adminPin by remember(adminConfig) { mutableStateOf(adminConfig?.adminPin ?: "") }

    // AI & Gemini Settings States
    var geminiApiKey by remember(adminConfig) { mutableStateOf(adminConfig?.geminiApiKey ?: "") }
    var geminiModelName by remember(adminConfig) { mutableStateOf(adminConfig?.geminiModelName ?: "gemini-3.5-flash") }
    var aiSystemPrompt by remember(adminConfig) { mutableStateOf(adminConfig?.aiSystemPrompt ?: "أنت المحاسب المالي والمساعد الذكي لمنصة OOZOX (oozox.com).") }
    var voiceSelection by remember(adminConfig) { mutableStateOf(adminConfig?.voiceSelection ?: "ar-EG-Standard-A") }
    var personaPrompt by remember(adminConfig) { mutableStateOf(adminConfig?.personaPrompt ?: "وكيل رقمي لإدارة الأعمال وتنسيق المحادثات والمجموعات والعمليات المحاسبية الفورية") }
    var biometricAuthEnabled by remember(adminConfig) { mutableStateOf(adminConfig?.biometricAuthEnabled ?: true) }

    // External APIs States
    var notificationUrl by remember(adminConfig) { mutableStateOf(adminConfig?.notificationApiUrl ?: "https://api.onesignal.com/v1/notifications") }
    var notificationKey by remember(adminConfig) { mutableStateOf(adminConfig?.notificationApiKey ?: "") }
    var newsUrl by remember(adminConfig) { mutableStateOf(adminConfig?.newsApiUrl ?: "https://api.goldnews.eg/v1") }
    var goldUrl by remember(adminConfig) { mutableStateOf(adminConfig?.goldPriceApiUrl ?: "https://api.goldprice.org/v1/egp") }
    var goldKey by remember(adminConfig) { mutableStateOf(adminConfig?.goldPriceApiKey ?: "") }
    var goldRefreshInterval by remember(adminConfig) { mutableIntStateOf(adminConfig?.goldPriceAutoRefreshIntervalSec ?: 15) }
    var encryptionEnabled by remember(adminConfig) { mutableStateOf(adminConfig?.encryptionEnabled ?: true) }

    // Price Update State
    var newGoldKarat21Price by remember { mutableStateOf(goldPrices.karat21.toString()) }

    // Dialog States
    var showNewPromoDialog by remember { mutableStateOf(false) }
    var selectedUserForEdit by remember { mutableStateOf<UserEntity?>(null) }
    var showUserEditDialog by remember { mutableStateOf(false) }
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()) }

    val pendingRequestsCount = remember(traderRequests, subscriptionRequests) {
        traderRequests.count { it.status == "PENDING" } + subscriptionRequests.count { it.status == "PENDING" }
    }

    // Auto-logout after 15 minutes (900,000 ms) of inactivity
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(lastInteractionTime) {
        while (isActive) {
            delay(15_000L) // Check every 15 seconds
            val idleDuration = System.currentTimeMillis() - lastInteractionTime
            if (idleDuration >= 15 * 60 * 1000L) { // 15 minutes timeout
                Toast.makeText(
                    context,
                    "تم تسجيل الخروج التلقائي من لوحة الإدارة لعدم النشاط لمدة 15 دقيقة",
                    Toast.LENGTH_LONG
                ).show()
                onBackClick()
                break
            }
        }
    }

    var settingsAutoSavedTime by remember { mutableStateOf("") }

    LaunchedEffect(
        appName, appLogoUri, adminPin,
        geminiApiKey, geminiModelName, aiSystemPrompt,
        notificationUrl, notificationKey, newsUrl,
        goldUrl, goldKey, goldRefreshInterval, encryptionEnabled,
        voiceSelection, personaPrompt, biometricAuthEnabled
    ) {
        delay(3000L) // Auto-save / Draft mechanism after 3 seconds of typing
        onSaveConfig(
            appName, appLogoUri, adminPin, geminiApiKey, geminiModelName, aiSystemPrompt,
            notificationUrl, notificationKey, newsUrl, goldUrl, goldKey, goldRefreshInterval, encryptionEnabled,
            voiceSelection, personaPrompt, biometricAuthEnabled
        )
        val format = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        settingsAutoSavedTime = "تم حفظ المسودة (تلقائياً): ${format.format(Date())}"
    }

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val primaryAccent = if (isDark) GoldPrimary else MaterialTheme.colorScheme.primary
    val onPrimaryAccent = if (isDark) Color.Black else MaterialTheme.colorScheme.onPrimary
    val primaryContainer = if (isDark) GoldPrimary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    val selectedTextCol = if (isDark) GoldAccent else MaterialTheme.colorScheme.primary
    val unselectedIconCol = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)

    BackHandler(enabled = drawerState.isOpen) {
        coroutineScope.launch { drawerState.close() }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                drawerContentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .widthIn(max = 320.dp)
                    .fillMaxHeight()
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Drawer Header: Admin Identity & System Status
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                        colors = listOf(
                                            if (isDark) GoldDark.copy(alpha = 0.35f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        )
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .border(
                                    1.dp,
                                    if (isDark) GoldDark.copy(alpha = 0.4f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                    RoundedCornerShape(16.dp)
                                )
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .background(primaryAccent, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AdminPanelSettings,
                                            contentDescription = null,
                                            tint = onPrimaryAccent,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = "لوحة إدارة OOZOX",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = primaryAccent
                                        )
                                        Text(
                                            text = "مدير المنظومة الأعلى (Admin)",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { coroutineScope.launch { drawerState.close() } },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "إغلاق القائمة",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = Color(0xFF10B981).copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "النظام محمي ومشفر",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF10B981),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }

                                Surface(
                                    color = if (isDark) GoldDark.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = if (adminPin.isNotBlank()) "PIN: مفعل" else "PIN: غير مضبوط",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = primaryAccent,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "أقسام الإدارة والتحكم",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryAccent,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    // Navigation Drawer Item 0: Metrics
                    item {
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.Analytics, contentDescription = null) },
                            label = { Text("المؤشرات والتقارير المالية", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) },
                            selected = selectedTab == 0,
                            onClick = {
                                selectedTab = 0
                                coroutineScope.launch { drawerState.close() }
                            },
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = primaryContainer,
                                selectedIconColor = primaryAccent,
                                selectedTextColor = selectedTextCol,
                                unselectedIconColor = unselectedIconCol,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurface
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // Navigation Drawer Item 1: Users
                    item {
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.People, contentDescription = null) },
                            label = { Text("المشتركون والمستخدمون", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) },
                            badge = {
                                Surface(
                                    color = if (selectedTab == 1) primaryAccent else MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = "${allUsers.size}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selectedTab == 1) onPrimaryAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                    )
                                }
                            },
                            selected = selectedTab == 1,
                            onClick = {
                                selectedTab = 1
                                coroutineScope.launch { drawerState.close() }
                            },
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = primaryContainer,
                                selectedIconColor = primaryAccent,
                                selectedTextColor = selectedTextCol,
                                unselectedIconColor = unselectedIconCol,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurface
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // Navigation Drawer Item 2: Plans & Offers
                    item {
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.CardMembership, contentDescription = null) },
                            label = { Text("باقات الاشتراكات والعروض", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) },
                            badge = {
                                if (promoAnnouncements.isNotEmpty()) {
                                    Surface(
                                        color = if (isDark) GoldAccent.copy(alpha = 0.25f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text(
                                            text = "${promoAnnouncements.size} عروض",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = primaryAccent,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            },
                            selected = selectedTab == 2,
                            onClick = {
                                selectedTab = 2
                                coroutineScope.launch { drawerState.close() }
                            },
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = primaryContainer,
                                selectedIconColor = primaryAccent,
                                selectedTextColor = selectedTextCol,
                                unselectedIconColor = unselectedIconCol,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurface
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // Navigation Drawer Item 3: Requests Center
                    item {
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.Inbox, contentDescription = null) },
                            label = { Text("مركز الطلبات والتوثيقات", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) },
                            badge = {
                                if (pendingRequestsCount > 0) {
                                    Surface(
                                        color = Color(0xFFEF4444),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text(
                                            text = "$pendingRequestsCount جديد",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            },
                            selected = selectedTab == 3,
                            onClick = {
                                selectedTab = 3
                                coroutineScope.launch { drawerState.close() }
                            },
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = primaryContainer,
                                selectedIconColor = primaryAccent,
                                selectedTextColor = selectedTextCol,
                                unselectedIconColor = unselectedIconCol,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurface
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // Navigation Drawer Item 4: Settings & Prices
                    item {
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                            label = { Text("أسعار الذهب وضبط النظام وAPI", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) },
                            selected = selectedTab == 4,
                            onClick = {
                                selectedTab = 4
                                coroutineScope.launch { drawerState.close() }
                            },
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = primaryContainer,
                                selectedIconColor = primaryAccent,
                                selectedTextColor = selectedTextCol,
                                unselectedIconColor = unselectedIconCol,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurface
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // Navigation Drawer Item 5: ZOX Broadcast Hub
                    item {
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.Campaign, contentDescription = null) },
                            label = { Text("بث الرسائل الجماعية (ZOX)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) },
                            badge = {
                                Surface(
                                    color = primaryAccent,
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = "الإدارة",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = onPrimaryAccent,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            },
                            selected = selectedTab == 5,
                            onClick = {
                                selectedTab = 5
                                coroutineScope.launch { drawerState.close() }
                            },
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = primaryContainer,
                                selectedIconColor = primaryAccent,
                                selectedTextColor = selectedTextCol,
                                unselectedIconColor = unselectedIconCol,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurface
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // Navigation Drawer Item 6: Tutorials & Guides
                    item {
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.School, contentDescription = null) },
                            label = { Text("الشروحات التعليمية", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) },
                            selected = selectedTab == 6,
                            onClick = {
                                selectedTab = 6
                                coroutineScope.launch { drawerState.close() }
                            },
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = primaryContainer,
                                selectedIconColor = primaryAccent,
                                selectedTextColor = selectedTextCol,
                                unselectedIconColor = unselectedIconCol,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurface
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // Navigation Drawer Item 7: Database Schema Integrity & Diagnostics
                    item {
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.Storage, contentDescription = null) },
                            label = { Text("سلامة وتكامل قاعدة البيانات (Room)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) },
                            badge = {
                                Surface(
                                    color = Color(0xFF10B981).copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = "V19",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF10B981),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            },
                            selected = selectedTab == 7,
                            onClick = {
                                selectedTab = 7
                                coroutineScope.launch { drawerState.close() }
                            },
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = primaryContainer,
                                selectedIconColor = primaryAccent,
                                selectedTextColor = selectedTextCol,
                                unselectedIconColor = unselectedIconCol,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurface
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // Divider & System Actions
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = GoldDark.copy(alpha = 0.3f), thickness = 0.8.dp)
                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "إجراءات المنظومة السريعة",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    item {
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.Refresh, contentDescription = null, tint = GoldPrimary) },
                            label = { Text("تحديث أسعار الذهب الفورية", fontSize = 12.sp) },
                            selected = false,
                            onClick = {
                                selectedTab = 4
                                coroutineScope.launch { drawerState.close() }
                            },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    item {
                        NavigationDrawerItem(
                            icon = { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color(0xFFF59E0B)) },
                            label = { Text("الخروج من لوحة الإدارة", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B)) },
                            selected = false,
                            onClick = {
                                coroutineScope.launch {
                                    drawerState.close()
                                    showLogoutConfirmDialog = true
                                }
                            },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // Footer
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "OOZOX Control Hub v2.4.0",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "نظام مشفر ومحمي بكلمة مرور الإدارة",
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                Column {
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    text = "لوحة التحكم والإدارة (Admin) ️",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = when (selectedTab) {
                                        0 -> "المؤشرات والتقارير المالية"
                                        1 -> "المشتركون وقاعدة المستخدمين"
                                        2 -> "الخطط والباقات الترويجية"
                                        3 -> "مركز الطلبات والتوثيق ($pendingRequestsCount)"
                                        4 -> "️ إعدادات النظام وأسعار الذهب وAPI"
                                        5 -> "مركز البث والرسائل الجماعية ZOX"
                                        6 -> "الشروحات التعليمية"
                                        7 -> "سلامة وتكامل قاعدة البيانات (Room V19)"
                                        else -> "لوحة التحكم"
                                    },
                                    fontSize = 11.sp,
                                    color = GoldPrimary
                                )
                            }
                        },
                        navigationIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            if (drawerState.isClosed) drawerState.open() else drawerState.close()
                                        }
                                    },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("admin_drawer_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Menu,
                                        contentDescription = "فتح القائمة الجانبية للوحة التحكم",
                                        tint = adaptiveGoldPrimary
                                    )
                                }
                                IconButton(
                                    onClick = onBackClick,
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "رجوع للتطبيق",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        },
                        actions = {
                            IconButton(onClick = {
                                coroutineScope.launch {
                                    isRefreshing = true
                                    delay(500)
                                    isRefreshing = false
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "تحديث بيانات وتقارير لوحة التحكم",
                                    tint = GoldPrimary
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                    )

                    ScrollableTabRow(
                        selectedTabIndex = selectedTab,
                        edgePadding = 8.dp,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = GoldPrimary,
                        indicator = { tabPositions ->
                            if (selectedTab < tabPositions.size) {
                                TabRowDefaults.SecondaryIndicator(
                                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                    height = 3.dp,
                                    color = GoldPrimary
                                )
                            }
                        }
                    ) {
                        val tabsList = listOf(
                            "المؤشرات" to 0,
                            "المشتركون (${allUsers.size})" to 1,
                            "الخطط والعروض" to 2,
                            "الطلبات ($pendingRequestsCount)" to 3,
                            "️ أسعار وإعدادات" to 4,
                            "بث ZOX" to 5,
                            "الشروحات" to 6,
                            "سلامة قاعدة البيانات" to 7
                        )

                        tabsList.forEach { (title, idx) ->
                            val isSelected = selectedTab == idx
                            Tab(
                                selected = isSelected,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedTab = idx
                                },
                                text = {
                                    Text(
                                        text = title,
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                        fontSize = 12.sp,
                                        color = if (isSelected) GoldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            )
                        }
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (isLoading || isInitialLoading || isRefreshing) {
                    AdminDashboardSkeleton(selectedTab = selectedTab)
                } else {
                    AnimatedContent(
                        targetState = selectedTab,
                        transitionSpec = {
                            (fadeIn(animationSpec = tween(220)) + scaleIn(initialScale = 0.98f))
                                .togetherWith(fadeOut(animationSpec = tween(150)) + scaleOut(targetScale = 0.98f))
                        },
                        label = "admin_tabs_anim",
                        modifier = Modifier.fillMaxSize()
                    ) { currentTab ->
                        when (currentTab) {
                        0 -> MetricsTab(allUsers, subscriptionRequests, allTransactions, allGoldPurchases, allProjects, allInvoices)
                        1 -> UsersTab(
                            users = allUsers,
                            onSelectUser = { user ->
                                selectedUserForEdit = user
                                showUserEditDialog = true
                            },
                            onDeleteUsers = onDeleteUsers
                        )
                        2 -> PlansAndOffersTab(
                            subscriptionPlans = subscriptionPlans,
                            promoAnnouncements = promoAnnouncements,
                            onUpdatePlan = onUpdatePlanPriceAndOffer,
                            onOpenNewPromo = { showNewPromoDialog = true },
                            onDeletePromo = onDeletePromoAnnouncement,
                            onTogglePromoStatus = onTogglePromoStatus
                        )
                        3 -> RequestsCenterTab(
                            traderRequests = traderRequests,
                            subscriptionRequests = subscriptionRequests,
                            onApproveTrader = onApproveTrader,
                            onRejectTrader = onRejectTrader,
                            onUpdateSubRequest = onUpdateSubscriptionRequestStatus
                        )
                        4 -> SettingsAndPricesTab(
                            appName = appName,
                            onAppNameChange = { appName = it },
                            appLogoUri = appLogoUri,
                            onAppLogoUriChange = { appLogoUri = it },
                            adminPin = adminPin,
                            onAdminPinChange = { adminPin = it },
                            geminiApiKey = geminiApiKey,
                            onGeminiApiKeyChange = { geminiApiKey = it },
                            geminiModelName = geminiModelName,
                            onGeminiModelNameChange = { geminiModelName = it },
                            aiSystemPrompt = aiSystemPrompt,
                            onAiSystemPromptChange = { aiSystemPrompt = it },
                            voiceSelection = voiceSelection,
                            onVoiceSelectionChange = { voiceSelection = it },
                            personaPrompt = personaPrompt,
                            onPersonaPromptChange = { personaPrompt = it },
                            biometricAuthEnabled = biometricAuthEnabled,
                            onBiometricAuthEnabledChange = { biometricAuthEnabled = it },
                            goldUrl = goldUrl,
                            onGoldUrlChange = { goldUrl = it },
                            goldKey = goldKey,
                            onGoldKeyChange = { goldKey = it },
                            goldRefreshInterval = goldRefreshInterval,
                            onGoldRefreshIntervalChange = { goldRefreshInterval = it },
                            goldPrices = goldPrices,
                            newPriceStr = newGoldKarat21Price,
                            onPriceStrChange = { newGoldKarat21Price = it },
                            onSavePrices = {
                                val price = newGoldKarat21Price.toDoubleOrNull()
                                if (price != null) onUpdatePrices(price)
                            },
                            notificationUrl = notificationUrl,
                            onNotificationUrlChange = { notificationUrl = it },
                            notificationKey = notificationKey,
                            onNotificationKeyChange = { notificationKey = it },
                            newsUrl = newsUrl,
                            onNewsUrlChange = { newsUrl = it },
                            encryptionEnabled = encryptionEnabled,
                            onEncryptionChange = { encryptionEnabled = it },
                            autoSavedDraftTime = settingsAutoSavedTime,
                            onSaveConfig = {
                                onSaveConfig(
                                    appName, appLogoUri, adminPin,
                                    geminiApiKey, geminiModelName, aiSystemPrompt,
                                    notificationUrl, notificationKey, newsUrl,
                                    goldUrl, goldKey, goldRefreshInterval, encryptionEnabled,
                                    voiceSelection, personaPrompt, biometricAuthEnabled
                                )
                            }
                        )
                        5 -> BroadcastHubTab(
                            allUsers = allUsers,
                            onSendBroadcast = { targetCategory, title, content, isUrgent, postAsAnnouncement, onComplete ->
                                if (onSendBroadcast != null) {
                                    onSendBroadcast(targetCategory, title, content, isUrgent, postAsAnnouncement, onComplete)
                                } else {
                                    onComplete(0)
                                }
                            }
                        )
                        6 -> TutorialsTab()
                        7 -> DatabaseIntegrityTab()
                        }
                    }
                }
            }
        }
    }

    // Modal Dialog: Edit User Subscription / Extend Trial
    if (showUserEditDialog && selectedUserForEdit != null) {
        val u = selectedUserForEdit!!
        var daysInput by remember { mutableStateOf("30") }
        var selectedTierName by remember { mutableStateOf(u.subscriptionTier) }

        AlertDialog(
            onDismissRequest = { showUserEditDialog = false },
            title = { Text("إدارة اشتراك المستخدم: ${u.name}", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("الهاتف: ${u.phone} | النوع: ${u.type}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Text("ترقية وتغيير الخطة مباشرة:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    SubscriptionTier.entries.forEach { tier ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RadioButton(
                                selected = selectedTierName == tier.name,
                                onClick = { selectedTierName = tier.name }
                            )
                            Text(tier.label, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }

                    OutlinedTextField(
                        value = daysInput,
                        onValueChange = { daysInput = it },
                        label = { Text("مدة التفعيل بالأيام") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    HorizontalDivider()

                    Button(
                        onClick = {
                            val days = daysInput.toLongOrNull() ?: 30L
                            onExtendUserTrial(u.id, days)
                            showUserEditDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StatusOrange),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.MoreTime, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تمديد الفترة التجريبية المجانية (+60/30 يوم)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val days = daysInput.toLongOrNull() ?: 30L
                        onUpdateUserTierDirectly(u.id, selectedTierName, days)
                        showUserEditDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = adaptiveGoldPrimary)
                ) {
                    Text("حفظ التغييرات", color = adaptiveOnPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUserEditDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Modal Dialog: New Promo Banner Announcement
    if (showNewPromoDialog) {
        var title by remember { mutableStateOf("") }
        var content by remember { mutableStateOf("") }
        var code by remember { mutableStateOf("") }
        var discountStr by remember { mutableStateOf("20") }

        AlertDialog(
            onDismissRequest = { showNewPromoDialog = false },
            title = { Text("إنشاء عرض وإعلان ترويجي جديد", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("عنوان العرض الترويجي") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        label = { Text("تفاصيل ووصف العرض") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it },
                        label = { Text("كود الخصم (اختياري)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = discountStr,
                        onValueChange = { discountStr = it },
                        label = { Text("نسبة الخصم %") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            onPostPromoAnnouncement(title, content, code.ifBlank { null }, discountStr.toIntOrNull() ?: 0, null)
                            showNewPromoDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary)
                ) {
                    Text("نشر العرض فوراً", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewPromoDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
        if (showLogoutConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutConfirmDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFF59E0B))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("تأكيد تسجيل الخروج", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                },
                text = { Text("هل أنت متأكد أنك تريد تسجيل الخروج من لوحة تحكم الإدارة؟", fontSize = 14.sp) },
                confirmButton = {
                    Button(
                        onClick = {
                            showLogoutConfirmDialog = false
                            onBackClick()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)) // Red for logout
                    ) {
                        Text("تسجيل الخروج", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutConfirmDialog = false }) {
                        Text("إلغاء", color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            )
        }
    }
}

@Composable
private fun MetricsTab(
    users: List<UserEntity>,
    subscriptionRequests: List<SubscriptionRequestEntity>,
    transactions: List<TransactionEntity>,
    purchases: List<GoldPurchaseEntity>,
    projects: List<ProjectEntity>,
    invoices: List<InvoiceEntity> = emptyList()
) {
    val context = LocalContext.current
    val totalUsers = users.size
    val freeTrialUsers = users.count { it.subscriptionTier == SubscriptionTier.FREE_TRIAL.name }
    val paidUsers = users.count { it.subscriptionTier != SubscriptionTier.FREE_TRIAL.name }
    val conversionRate = if (totalUsers > 0) (paidUsers.toDouble() / totalUsers * 100).toInt() else 0

    // Financial Reporting Processing Unit by Payment Method
    val paymentReport = remember(invoices) {
        SmartProductScannerEngine.generatePaymentBreakdownReport(invoices)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("مؤشرات وأداء المنصة", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = GoldPrimary)
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard("إجمالي مستخدمي المنصة", "$totalUsers مستخدم", Icons.Default.Group, adaptiveGoldPrimary, Modifier.weight(1f))
                MetricCard("المشتركون بالمدفوع", "$paidUsers مشتركون", Icons.Default.WorkspacePremium, adaptiveStatusGreen, Modifier.weight(1f))
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard("النسخة المجانية (60 يوم)", "$freeTrialUsers مستخدم", Icons.Default.Timer, adaptiveStatusOrange, Modifier.weight(1f))
                MetricCard("نسبة التحويل", "$conversionRate%", Icons.Default.TrendingUp, adaptiveGoldPrimary, Modifier.weight(1f))
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard("إجمالي صفقات الذهب", "${transactions.size + purchases.size} صفقة", Icons.Default.ReceiptLong, adaptiveGoldPrimary, Modifier.weight(1f))
                MetricCard("المشاريع النشطة", "${projects.size} مشروع", Icons.Default.Business, adaptiveStatusGreen, Modifier.weight(1f))
            }
        }

        // --- Financial Reporting Processing Unit (تصنيف الحسابات بناءً على وسيلة الدفع للأدمن) ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.2.dp, GoldPrimary.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("وحدة معالجة التقارير المالية وتصنيف الحسابات 💳", fontSize = 14.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Text("تصنيف الإيرادات حسب وسيلة الدفع المعتمدة (إنستاباي، فودافون كاش، كاش، تحويل بنكي)", fontSize = 11.sp, color = GoldPrimary)
                            }
                        }

                        IconButton(
                            onClick = {
                                val file = SmartProductScannerEngine.exportPaymentReportToCsv(
                                    context = context,
                                    report = paymentReport,
                                    shopName = "منظومة OOZOX المالية"
                                )
                                if (file != null) {
                                    SimpleExcelEngine.shareExcelFile(context, file, "تقرير تصنيف وسائل الدفع للأدمن - Excel")
                                } else {
                                    Toast.makeText(context, "تعذر تصدير الملف", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Icon(Icons.Default.Download, contentDescription = "تصدير كشف الحسابات", tint = Color(0xFF10B981))
                        }
                    }

                    // Breakdown cards grid
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AdminPaymentStatCard("إنستا باي (InstaPay)", "${paymentReport.grandTotalInstaPay.toInt()} ج.م", Color(0xFF8B5CF6), Modifier.weight(1f))
                        AdminPaymentStatCard("فودافون كاش / محافظ", "${paymentReport.grandTotalVodafoneCash.toInt()} ج.م", Color(0xFFEF4444), Modifier.weight(1f))
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AdminPaymentStatCard("كاش نقدي بالدرج", "${paymentReport.grandTotalCash.toInt()} ج.م", Color(0xFF10B981), Modifier.weight(1f))
                        AdminPaymentStatCard("تحويلات بنكية وفيزا", "${(paymentReport.grandTotalBankTransfer + paymentReport.grandTotalVisa).toInt()} ج.م", Color(0xFF3B82F6), Modifier.weight(1f))
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AdminPaymentStatCard("مدفوعات الموردين", "${paymentReport.grandTotalSupplierPayout.toInt()} ج.م", Color(0xFFF59E0B), Modifier.weight(1f))
                        AdminPaymentStatCard("صافي النقدية المتبقية", "${paymentReport.netStoreCashflow.toInt()} ج.م", GoldPrimary, Modifier.weight(1f))
                    }

                    // Detailed cashier table
                    if (paymentReport.cashierBreakdowns.isNotEmpty()) {
                        Text("تفصيل الحسابات ومبيعات كل موظف حسب وسيلة الدفع:", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            paymentReport.cashierBreakdowns.forEach { c ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("👤 الموظف: ${c.cashierName}", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = GoldPrimary)
                                            Text("إجمالي: ${c.totalSales.toInt()} ج.م (${c.invoicesCount} فاتورة)", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("• إنستاباي: ${c.instaPayTotal.toInt()} ج.م", fontSize = 10.5.sp, color = Color(0xFF8B5CF6))
                                            Text("• فودافون كاش: ${c.vodafoneCashTotal.toInt()} ج.م", fontSize = 10.5.sp, color = Color(0xFFEF4444))
                                            Text("• كاش: ${c.cashInDrawerTotal.toInt()} ج.م", fontSize = 10.5.sp, color = Color(0xFF10B981))
                                        }

                                        if (c.supplierPayoutTotal > 0) {
                                            Text("• مدفوع لموردين: ${c.supplierPayoutTotal.toInt()} ج.م | صافي العهدة: ${c.netCashInHand.toInt()} ج.م", fontSize = 10.5.sp, color = Color(0xFFF59E0B), fontWeight = FontWeight.Medium)
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Text(
                            "لا توجد فواتير ومبيعات مسجلة في النظام اليوم بعد.",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminPaymentStatCard(
    title: String,
    amount: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(4.dp))
            Text(amount, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = accentColor, maxLines = 1, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun MetricCard(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(10.dp))
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(4.dp))
            Text(title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun UsersTab(
    users: List<UserEntity>,
    onSelectUser: (UserEntity) -> Unit,
    onDeleteUsers: (List<String>) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredUsers = users.filter { it.name.contains(searchQuery, true) || it.phone.contains(searchQuery) }
    
    var selectedUserIds by remember { mutableStateOf(setOf<String>()) }
    val isAllSelected = filteredUsers.isNotEmpty() && selectedUserIds.size == filteredUsers.size

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("بحث بالاسم أو الهاتف") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = GoldPrimary) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = isAllSelected,
                        onCheckedChange = { checked ->
                            if (checked) {
                                selectedUserIds = filteredUsers.map { it.id }.toSet()
                            } else {
                                selectedUserIds = emptySet()
                            }
                        },
                        colors = CheckboxDefaults.colors(checkedColor = GoldPrimary)
                    )
                    Text("تحديد الكل", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                
                if (selectedUserIds.isNotEmpty()) {
                    OutlinedButton(
                        onClick = {
                            onDeleteUsers(selectedUserIds.toList())
                            selectedUserIds = emptySet()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                        border = BorderStroke(1.dp, Color(0xFFEF4444))
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("حذف المحدد (${selectedUserIds.size})", fontSize = 12.sp)
                    }
                }
            }
        }

        items(filteredUsers, key = { it.id }) { u ->
            val isSelected = selectedUserIds.contains(u.id)
            Card(
                colors = CardDefaults.cardColors(containerColor = if (isSelected) GoldPrimary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp),
                border = if (isSelected) BorderStroke(1.dp, GoldPrimary) else null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { checked ->
                            if (checked) {
                                selectedUserIds = selectedUserIds + u.id
                            } else {
                                selectedUserIds = selectedUserIds - u.id
                            }
                        },
                        colors = CheckboxDefaults.colors(checkedColor = GoldPrimary)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(u.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                                Text("الهاتف: ${u.phone} | ${u.type}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            val tierEnum = SubscriptionTier.entries.find { it.name == u.subscriptionTier } ?: SubscriptionTier.FREE_TRIAL
                            Surface(
                                color = GoldPrimary.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    tierEnum.badgeText,
                                    fontSize = 11.sp,
                                    color = GoldPrimary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Button(
                                onClick = { onSelectUser(u) },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("تعديل الاشتراك / التمديد", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlansAndOffersTab(
    subscriptionPlans: List<SubscriptionPlanEntity>,
    promoAnnouncements: List<PromoAnnouncementEntity>,
    onUpdatePlan: (planId: String, monthlyPrice: Double, discountPercent: Int, offerTitle: String?, isOfferActive: Boolean) -> Unit,
    onOpenNewPromo: () -> Unit,
    onDeletePromo: (String) -> Unit,
    onTogglePromoStatus: (List<String>, Boolean) -> Unit
) {
    var selectedPromoIds by remember { mutableStateOf(setOf<String>()) }
    val isAllSelected = promoAnnouncements.isNotEmpty() && selectedPromoIds.size == promoAnnouncements.size

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("إدارة الخطط والخصومات", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = GoldPrimary)
                Button(
                    onClick = onOpenNewPromo,
                    colors = ButtonDefaults.buttonColors(containerColor = StatusOrange)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("إعلان عرض ترويجي", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Promo Announcements list
        if (promoAnnouncements.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = isAllSelected,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    selectedPromoIds = promoAnnouncements.map { it.id }.toSet()
                                } else {
                                    selectedPromoIds = emptySet()
                                }
                            },
                            colors = CheckboxDefaults.colors(checkedColor = GoldPrimary)
                        )
                        Text("تحديد الكل", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    if (selectedPromoIds.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    onTogglePromoStatus(selectedPromoIds.toList(), true)
                                    selectedPromoIds = emptySet()
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusGreen),
                                border = BorderStroke(1.dp, StatusGreen)
                            ) {
                                Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("تفعيل", fontSize = 12.sp)
                            }
                            OutlinedButton(
                                onClick = {
                                    onTogglePromoStatus(selectedPromoIds.toList(), false)
                                    selectedPromoIds = emptySet()
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusRed),
                                border = BorderStroke(1.dp, StatusRed)
                            ) {
                                Icon(Icons.Default.VisibilityOff, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("إخفاء", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            items(promoAnnouncements, key = { it.id }) { promo ->
                val isSelected = selectedPromoIds.contains(promo.id)
                Card(
                    colors = CardDefaults.cardColors(containerColor = if (isSelected) GoldPrimary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    border = if (isSelected) BorderStroke(1.dp, GoldPrimary) else null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    selectedPromoIds = selectedPromoIds + promo.id
                                } else {
                                    selectedPromoIds = selectedPromoIds - promo.id
                                }
                            },
                            colors = CheckboxDefaults.colors(checkedColor = GoldPrimary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(promo.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                if (promo.isActive) {
                                    Surface(color = StatusGreen.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                                        Text("نشط", color = StatusGreen, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                    }
                                } else {
                                    Surface(color = StatusRed.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                                        Text("مخفي", color = StatusRed, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                    }
                                }
                            }
                            Text(promo.content, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (promo.promoCode != null) {
                                Text("كود الخصم: ${promo.promoCode} (خصم ${promo.discountPercent}%)", fontSize = 11.sp, color = GoldPrimary, fontWeight = FontWeight.Bold)
                            }
                        }
                        IconButton(onClick = { onDeletePromo(promo.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = StatusRed)
                        }
                    }
                }
            }
        } else {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Image(
                            painter = painterResource(id = R.drawable.announcements_empty_state),
                            contentDescription = "لا توجد إعلانات",
                            modifier = Modifier
                                .size(180.dp)
                                .clip(RoundedCornerShape(16.dp))
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text("لا توجد إعلانات نشطة", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("يمكنك إضافة عروض ترويجية وإعلانات لتظهر للمستخدمين", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                }
            }
        }

        item {
            Text("تعديل أسعار وعروض الخطط الـ 4:", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }

        items(subscriptionPlans, key = { it.id }) { plan ->
            var priceStr by remember(plan) { mutableStateOf(plan.monthlyPrice.toString()) }
            var discountStr by remember(plan) { mutableStateOf(plan.discountPercent.toString()) }
            var offerTitleStr by remember(plan) { mutableStateOf(plan.offerTitle ?: "") }
            var isOfferActive by remember(plan) { mutableStateOf(plan.isOfferActive) }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(plan.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = GoldPrimary)

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = priceStr,
                            onValueChange = { priceStr = it },
                            label = { Text("السعر الشهري (ج.م)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = discountStr,
                            onValueChange = { discountStr = it },
                            label = { Text("الخصم %") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = offerTitleStr,
                        onValueChange = { offerTitleStr = it },
                        label = { Text("عنوان العرض / البادج الترويجي") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("تفعيل العرض والخصم لهذه الخطة", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Switch(
                            checked = isOfferActive,
                            onCheckedChange = { isOfferActive = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = GoldPrimary)
                        )
                    }

                    Button(
                        onClick = {
                            onUpdatePlan(
                                plan.id,
                                priceStr.toDoubleOrNull() ?: plan.monthlyPrice,
                                discountStr.toIntOrNull() ?: 0,
                                offerTitleStr.ifBlank { null },
                                isOfferActive
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("حفظ تعديلات الخطة", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun RequestsCenterTab(
    traderRequests: List<TraderRequestEntity>,
    subscriptionRequests: List<SubscriptionRequestEntity>,
    onApproveTrader: (String) -> Unit,
    onRejectTrader: (String) -> Unit,
    onUpdateSubRequest: (requestId: String, status: String, notes: String?, userId: String?, planId: String?) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section A: Subscriptions Payment Requests
        item {
            Text("طلبات تفعيل اشتراكات المستخدمين", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = GoldPrimary)
        }

        if (subscriptionRequests.isEmpty()) {
            item {
                Text("لا توجد طلبات اشتراك معلقة حالياً", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            items(subscriptionRequests, key = { it.id }) { req ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(req.userName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Surface(
                                color = if (req.status == "PENDING") StatusOrange.copy(alpha = 0.2f) else StatusGreen.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(req.status, fontSize = 11.sp, color = if (req.status == "PENDING") StatusOrange else StatusGreen, modifier = Modifier.padding(4.dp))
                            }
                        }
                        Text("الخطة المطلوبة: ${req.requestedPlanTitle}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = GoldPrimary)
                        Text("المبلغ المدفوع: ${req.pricePaid.toInt()} ج.م | الهاتف: ${req.userPhone}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        if (req.status == "PENDING") {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = { onUpdateSubRequest(req.id, "APPROVED", "تم التفعيل بواسطة الادمن", req.userId, req.requestedPlanId) },
                                    colors = ButtonDefaults.buttonColors(containerColor = StatusGreen),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("تأكيد وتفعيل الاشتراك", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = { onUpdateSubRequest(req.id, "REJECTED", "مرفوض", null, null) },
                                    colors = ButtonDefaults.buttonColors(containerColor = StatusRed),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("رفض", color = Color.White, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section B: Trader Verification Requests
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Text("طلبات توثيق تجار الذهب", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = GoldPrimary)
        }

        if (traderRequests.isEmpty()) {
            item {
                Text("لا توجد طلبات توثيق تجار معلقة حالياً", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            items(traderRequests, key = { it.id }) { req ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(req.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(req.status, fontSize = 11.sp, color = GoldPrimary)
                        }
                        Text("المحل: ${req.shopName} - ${req.shopAddress}", fontSize = 12.sp)
                        Text("الترخيص والهاتف: ${req.license} | ${req.phone}", fontSize = 12.sp)

                        if (req.status == TraderStatus.PENDING.name) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = { onApproveTrader(req.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = StatusGreen),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("موافقة وتوثيق", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = { onRejectTrader(req.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = StatusRed),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("رفض", color = Color.White, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsAndPricesTab(
    appName: String,
    onAppNameChange: (String) -> Unit,
    appLogoUri: String,
    onAppLogoUriChange: (String) -> Unit,
    adminPin: String,
    onAdminPinChange: (String) -> Unit,
    geminiApiKey: String,
    onGeminiApiKeyChange: (String) -> Unit,
    geminiModelName: String,
    onGeminiModelNameChange: (String) -> Unit,
    aiSystemPrompt: String,
    onAiSystemPromptChange: (String) -> Unit,
    voiceSelection: String,
    onVoiceSelectionChange: (String) -> Unit,
    personaPrompt: String,
    onPersonaPromptChange: (String) -> Unit,
    biometricAuthEnabled: Boolean,
    onBiometricAuthEnabledChange: (Boolean) -> Unit,
    goldUrl: String,
    onGoldUrlChange: (String) -> Unit,
    goldKey: String,
    onGoldKeyChange: (String) -> Unit,
    goldRefreshInterval: Int,
    onGoldRefreshIntervalChange: (Int) -> Unit,
    goldPrices: GoldPricesModel,
    newPriceStr: String,
    onPriceStrChange: (String) -> Unit,
    onSavePrices: () -> Unit,
    notificationUrl: String,
    onNotificationUrlChange: (String) -> Unit,
    notificationKey: String,
    onNotificationKeyChange: (String) -> Unit,
    newsUrl: String,
    onNewsUrlChange: (String) -> Unit,
    encryptionEnabled: Boolean,
    onEncryptionChange: (Boolean) -> Unit,
    autoSavedDraftTime: String = "",
    onSaveConfig: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var isTestingAi by remember { mutableStateOf(false) }
    var aiTestResult by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section 1: White Label Branding
        item {
            Text("تخصيص الهوية والاسم واللوجو للمنصة (White-Label) ️", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = GoldPrimary)
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("اسم المشروع والتطبيقات المنسوبة (يظهر بكافة القوائم ومحركات البحث)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)

                    OutlinedTextField(
                        value = appName,
                        onValueChange = onAppNameChange,
                        label = { Text("اسم التطبيق الرئيسي (App Name)") },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = GoldPrimary) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = appLogoUri,
                        onValueChange = onAppLogoUriChange,
                        label = { Text("رابط / مسار صورة اللوجو (Logo Image URL)") },
                        leadingIcon = { Icon(Icons.Default.Image, contentDescription = null, tint = GoldPrimary) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = adminPin,
                        onValueChange = onAdminPinChange,
                        label = { Text("رمز كود المرور الخاص بالأدمن (Admin Access PIN)") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = GoldPrimary) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Surface(
                        color = GoldDark,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = GoldAccent)
                            Text(
                                "المعاينة الفورية: الاسم الحالي هو \"$appName\". سيتغير هذا الاسم واللوجو ديناميكياً بكل شاشات المنصة فور الضغط على حفظ.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Button(
                        onClick = onSaveConfig,
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("حفظ الهوية والاسم واللوجو الجديد", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Section 2: Advanced Accounting Services Management
        item {
            Text("مركز إدارة الخدمات المالية المتقدمة", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = GoldPrimary)
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("إعداد مفاتيح الربط وشخصية المساعد المالي:", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                    OutlinedTextField(
                        value = geminiApiKey,
                        onValueChange = onGeminiApiKeyChange,
                        label = { Text("مفتاح الخدمة (أدخله لتفعيل التحليل المالي المباشر)") },
                        leadingIcon = { Icon(Icons.Default.Key, contentDescription = null, tint = GoldPrimary) },
                        singleLine = true,
                        placeholder = { Text("مفتاح الوصول...") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("اختر محرك التحليل المالي:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "gemini-3.5-flash" to "قياسي",
                            "gemini-3.1-pro-preview" to "متقدم",
                            "gemini-3.1-flash-lite-preview" to "سريع"
                        ).forEach { (model, label) ->
                            val isSelected = geminiModelName == model
                            FilterChip(
                                selected = isSelected,
                                onClick = { onGeminiModelNameChange(model) },
                                label = { Text(label, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = GoldPrimary,
                                    selectedLabelColor = Color.Black
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = aiSystemPrompt,
                        onValueChange = onAiSystemPromptChange,
                        label = { Text("توجيهات مساعدك المالي (أسلوب العمل):") },
                        leadingIcon = { Icon(Icons.Default.SupportAgent, contentDescription = null, tint = GoldPrimary) },
                        minLines = 3,
                        maxLines = 5,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                isTestingAi = true
                                aiTestResult = null
                                coroutineScope.launch {
                                    val testConfig = AdminConfigEntity(
                                        geminiApiKey = geminiApiKey,
                                        geminiModelName = geminiModelName,
                                        aiSystemPrompt = aiSystemPrompt,
                                        voiceSelection = voiceSelection,
                                        personaPrompt = personaPrompt,
                                        biometricAuthEnabled = biometricAuthEnabled
                                    )
                                    val result = com.example.util.GeminiAiService.queryGeminiAi(
                                        userQuery = "مرحباً، هل يعمل اتصال الخدمات المحاسبية المتقدمة بشكل صحيح؟",
                                        contextInfo = "اختبار الأدمن اللحظي",
                                        adminConfig = testConfig
                                    )
                                    aiTestResult = result
                                    isTestingAi = false
                                }
                            },
                            enabled = !isTestingAi,
                            colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                            modifier = Modifier.weight(1f)
                        ) {
                            if (isTestingAi) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.Black, strokeWidth = 2.dp)
                            } else {
                                Text("اختبار اتصال المحرك", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }

                        Button(
                            onClick = onSaveConfig,
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("حفظ إعدادات المساعد", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    if (aiTestResult != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.background,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().border(1.dp, GoldPrimary, RoundedCornerShape(10.dp))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("نتيجة اختبار المساعد المالي:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = GoldPrimary)
                                Text(aiTestResult ?: "", fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground)
                            }
                        }
                    }
                }
            }
        }

        // Section 2.5: Business Voice Assistant Settings
        item {
            Text("إعدادات المساعد الصوتي للأعمال 🎙️⚡", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = OozoxTeal)
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("تخصيص صوت وشخصية الوكيل الصوتي وحماية البصمة:", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                    Text("نبرة وصوت الوكيل الصوتي (Voice Selection):", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "ar-EG-Standard-A" to "صوت نسائي (مصر)",
                            "ar-EG-Standard-B" to "صوت رجالي (مصر)",
                            "ar-XA-Standard-A" to "عربي فصحى"
                        ).forEach { (voiceId, label) ->
                            val isSelected = voiceSelection == voiceId
                            FilterChip(
                                selected = isSelected,
                                onClick = { onVoiceSelectionChange(voiceId) },
                                label = { Text(label, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = OozoxTeal,
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = personaPrompt,
                        onValueChange = onPersonaPromptChange,
                        label = { Text("توجيهات شخصية الوكيل الصوتي (Persona Prompt)") },
                        leadingIcon = { Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = OozoxTeal) },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Default.Fingerprint, contentDescription = null, tint = OozoxTeal)
                            Column {
                                Text("التعرف بالبصمة الحيوية / الصوتية", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("تأكيد هوية المستخدم قبل تنفيذ عمليات الخصم والقيود", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Switch(
                            checked = biometricAuthEnabled,
                            onCheckedChange = onBiometricAuthEnabledChange,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = OozoxTeal
                            )
                        )
                    }

                    Button(
                        onClick = onSaveConfig,
                        colors = ButtonDefaults.buttonColors(containerColor = OozoxTeal),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("حفظ إعدادات الوكيل الصوتي", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Section 3: Live Gold & Silver Price API Management
        item {
            Text("إدارة APIs أسعار الذهب والفضة المباشرة", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = GoldPrimary)
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("رابط مزود الأسعار والرموز والجدولة اللحظية:", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                    OutlinedTextField(
                        value = goldUrl,
                        onValueChange = onGoldUrlChange,
                        label = { Text("رابط API أسعار الذهب (Gold Price Endpoint)") },
                        leadingIcon = { Icon(Icons.Default.CloudSync, contentDescription = null, tint = GoldPrimary) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = goldKey,
                        onValueChange = onGoldKeyChange,
                        label = { Text("رمز API Key لمزود الأسعار (إن وجد)") },
                        leadingIcon = { Icon(Icons.Default.Key, contentDescription = null, tint = GoldPrimary) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("معدل التحديث التلقائي للأسعار بالثواني:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(5, 15, 30, 60).forEach { interval ->
                            FilterChip(
                                selected = goldRefreshInterval == interval,
                                onClick = { onGoldRefreshIntervalChange(interval) },
                                label = { Text("$interval ثوانٍ", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = GoldPrimary,
                                    selectedLabelColor = Color.Black
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    HorizontalDivider(color = Color.DarkGray)

                    Text("تعديل يدوي مباشر لسعر جرام عيار 21 (الآن: ${goldPrices.karat21} ج.م):", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = newPriceStr,
                            onValueChange = onPriceStrChange,
                            label = { Text("السعر (ج.م)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = onSavePrices,
                            colors = ButtonDefaults.buttonColors(containerColor = StatusGreen)
                        ) {
                            Text("تحديث فوراً", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Section 4: Notifications & News API
        item {
            Text("إعدادات APIs الإشعارات والأخبار والشبكة", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = GoldPrimary)
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = notificationUrl,
                        onValueChange = onNotificationUrlChange,
                        label = { Text("رابط خدمة الإشعارات (OneSignal / FCM Endpoint)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = notificationKey,
                        onValueChange = onNotificationKeyChange,
                        label = { Text("مفتاح API Token للإشعارات") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newsUrl,
                        onValueChange = onNewsUrlChange,
                        label = { Text("رابط خدمة الأخبار والتوصيات (News API)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("تشفير البيانات القياسية وحمايتها", fontSize = 13.sp)
                        Switch(
                            checked = encryptionEnabled,
                            onCheckedChange = onEncryptionChange,
                            colors = SwitchDefaults.colors(checkedThumbColor = GoldPrimary)
                        )
                    }

                    Button(
                        onClick = onSaveConfig,
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("حفظ جميع إعدادات APIs والربط", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                    
                    if (autoSavedDraftTime.isNotEmpty()) {
                        Text(
                            text = autoSavedDraftTime,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }

        // Section 4.5: FCM Real-time Push Notifications Diagnostic & Test Center
        item {
            val context = LocalContext.current
            val clipboardManager = LocalClipboardManager.current
            val fcmToken by FcmNotificationManager.fcmToken.collectAsStateWithLifecycle()
            val isPushActive by FcmNotificationManager.isPushEnabled.collectAsStateWithLifecycle()

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = GoldPrimary)
                            Text("مركز فحص وتجربة إشعارات FCM", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = GoldPrimary)
                        }
                        Surface(
                            color = if (isPushActive) StatusGreen.copy(alpha = 0.15f) else Color.Red.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                if (isPushActive) "FCM متصل ونشط" else "معطل",
                                fontSize = 11.sp,
                                color = if (isPushActive) StatusGreen else Color.Red,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text("رمز تسجيل الجهاز الحالي (FCM Device Token):", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = fcmToken ?: "جاري تهيئة رمز FCM من خدمات Google Play...",
                                fontSize = 11.sp,
                                color = if (fcmToken != null) adaptiveGoldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                modifier = Modifier.weight(1f)
                            )
                            if (fcmToken != null) {
                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(fcmToken ?: ""))
                                        Toast.makeText(context, "تم نسخ رمز FCM بنجاح!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "نسخ", tint = adaptiveGoldPrimary, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }

                    Text("اختبار إرسال إشعارات فورية مع الربط المباشر (Deep-links):", fontSize = 12.sp, fontWeight = FontWeight.Bold)

                    // Simulator Button 1: Chat Message
                    OutlinedButton(
                        onClick = {
                            FcmNotificationManager.sendChatMessageNotification(
                                context = context,
                                chatId = "direct_sample",
                                senderName = "م. أحمد الشامي (تاجر ذهب)",
                                messageText = "تم إيداع الدفعة المتفق عليها لحسابك الآن، يرجى التأكيد"
                            )
                            Toast.makeText(context, "تم إرسال إشعار رسالة جديدة", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldPrimary)
                    ) {
                        Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("تجربة إشعار رسالة محادثة جديدة", fontSize = 12.sp)
                    }

                    // Simulator Button 2: Project Update
                    OutlinedButton(
                        onClick = {
                            FcmNotificationManager.sendProjectUpdateNotification(
                                context = context,
                                projectId = "proj_sample",
                                projectName = "برج الأندلس - القاهرة",
                                title = "اعتماد ميزانية جديدة ️",
                                updateMessage = "تمت الموافقة على بند توريد الحديد بقيمة 120,000 ج.م من قبل المشرف."
                            )
                            Toast.makeText(context, "تم إرسال إشعار تحديث مشروع ️", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusGreen)
                    ) {
                        Icon(Icons.Default.Engineering, contentDescription = null, tint = StatusGreen, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("تجربة إشعار تحديث ميزانية مشروع ️", fontSize = 12.sp, color = StatusGreen)
                    }

                    // Simulator Button 3: Gold Deal
                    OutlinedButton(
                        onClick = {
                            FcmNotificationManager.sendGoldDealNotification(
                                context = context,
                                transactionId = "tx_sample",
                                title = "تأكيد صفقة ذهب عيار 21",
                                message = "تم تأكيد بيع 150 جرام عيار 21 بسعر 540,000 ج.م بنجاح."
                            )
                            Toast.makeText(context, "تم إرسال إشعار صفقة ذهب", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldAccent)
                    ) {
                        Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("تجربة إشعار صفقة ذهب ومبيعات", fontSize = 12.sp, color = GoldAccent)
                    }

                    // Simulator Button 4: Gold Price Threshold Alert
                    OutlinedButton(
                        onClick = {
                            com.example.util.NotificationHelper.showGoldPriceThresholdNotification(
                                context = context,
                                alertId = "admin_test_alert",
                                metalType = "GOLD",
                                karat = "21",
                                currentPrice = 3950.0,
                                targetPrice = 3900.0,
                                direction = "ABOVE",
                                note = "توقيت شراء سبائك عيار 21"
                            )
                            Toast.makeText(context, "تم إرسال إشعار تنبيه السعر", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldPrimary)
                    ) {
                        Icon(Icons.Default.AddAlert, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("تجربة إشعار بلوغ السعر المستهدف (Price Alert)", fontSize = 12.sp, color = GoldPrimary)
                    }
                }
            }
        }

        // Section 5: App Store & Google Play Readiness Check
        item {
            Text("جاهزية النشر بمتجري Google Play & Apple App Store", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = GoldPrimary)
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("شروط القبول ومتطلبات المتاجر الرسمية المطبقة بالكامل:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("1. خيار حذف الحساب والبيانات (Delete Account): متوفر بتبويب الملف الشخصي وفقاً لسياسة Google Play & iOS Guideline 5.1.1(v).", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("2. روابط سياسة الخصوصية واستعادة المشتريات (Restore Purchases): متوفرة بجميع واجهات الاشتراكات والتسجيل.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("3. نظام البلاغات وحظر المستخدمين (UGC Moderation): مفعل لمنع السلوكيات غير اللائقة بالمحادثات.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("4. التشفير والحماية: مفعل لتأمين معاملات الذهب والفضة وقيود دفتر الأستاذ بنجاح.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun BroadcastHubTab(
    allUsers: List<UserEntity>,
    onSendBroadcast: (
        targetCategory: BroadcastTargetCategory,
        title: String,
        content: String,
        isUrgent: Boolean,
        postAsAnnouncement: Boolean,
        onComplete: (Int) -> Unit
    ) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("admin_drafts", android.content.Context.MODE_PRIVATE) }

    var selectedCategory by remember { mutableStateOf(BroadcastTargetCategory.ALL_USERS) }
    var broadcastTitle by remember { mutableStateOf(prefs.getString("broadcast_title", "") ?: "") }
    var broadcastContent by remember { mutableStateOf(prefs.getString("broadcast_content", "") ?: "") }
    var isUrgent by remember { mutableStateOf(false) }
    var postAsAnnouncement by remember { mutableStateOf(true) }
    var isSending by remember { mutableStateOf(false) }
    var lastSentCount by remember { mutableStateOf<Int?>(null) }
    var lastSavedTime by remember { mutableStateOf("") }

    LaunchedEffect(broadcastTitle, broadcastContent) {
        delay(2000L) // 2 seconds debounce
        if (broadcastTitle.isNotEmpty() || broadcastContent.isNotEmpty()) {
            prefs.edit()
                .putString("broadcast_title", broadcastTitle)
                .putString("broadcast_content", broadcastContent)
                .apply()
            
            val format = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            lastSavedTime = "تم الحفظ كمسودة: ${format.format(Date())}"
        }
    }

    val traderCount = remember(allUsers) { allUsers.count { it.type == UserType.GOLD_TRADER.name } }
    val businessCount = remember(allUsers) { allUsers.count { it.type == UserType.BUSINESSMAN.name } }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. ZOX Account Master Banner
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = GoldDark.copy(alpha = 0.35f)
                ),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(GoldPrimary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Campaign,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "حساب الإدارة العليا المركزي",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = GoldPrimary
                                )
                                Surface(
                                    color = GoldAccent.copy(alpha = 0.25f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "ADMIN",
                                        color = GoldAccent,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "قناة البث الرسمية المعتمدة عبر الخادم",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    HorizontalDivider(color = GoldDark.copy(alpha = 0.3f), thickness = 0.8.dp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.PhoneDisabled, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                            Text("المكالمات الواردة: محظورة تماماً", fontSize = 11.sp, color = Color(0xFFEF4444), fontWeight = FontWeight.SemiBold)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                            Text("الرسائل النصية والبث: مفعلة", fontSize = 11.sp, color = Color(0xFF10B981), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // 2. Select Target Category
        item {
            Text(
                text = "1. حدد الفئة المستهدفة للإرسال الجماعي",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                BroadcastTargetCategory.values().forEach { category ->
                    val isSelected = selectedCategory == category
                    val recipientCount = when (category) {
                        BroadcastTargetCategory.ALL_USERS -> allUsers.size
                        BroadcastTargetCategory.GOLD_TRADERS -> traderCount
                        BroadcastTargetCategory.BUSINESSMEN -> businessCount
                        BroadcastTargetCategory.SUB_ACCOUNTS -> allUsers.size
                        BroadcastTargetCategory.WORKGROUPS -> allUsers.size
                        BroadcastTargetCategory.ACTIVE_RECENTLY -> allUsers.size
                    }

                    Card(
                        onClick = { selectedCategory = category },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) GoldPrimary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) GoldPrimary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedCategory = category },
                                    colors = RadioButtonDefaults.colors(selectedColor = GoldPrimary)
                                )
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(category.icon, fontSize = 16.sp)
                                        Text(
                                            text = category.label,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = if (isSelected) GoldPrimary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Text(
                                        text = category.description,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Surface(
                                color = if (isSelected) GoldPrimary else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "$recipientCount مستخدم",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Message Composition
        item {
            Text(
                text = "2. نص وبيانات الرسالة الجماعية ️",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = broadcastTitle,
                        onValueChange = { broadcastTitle = it },
                        label = { Text("عنوان التوجيه / الإشعار الإداري") },
                        placeholder = { Text("تحديث الأسعار") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.Title, contentDescription = null, tint = GoldPrimary)
                        }
                    )

                    OutlinedTextField(
                        value = broadcastContent,
                        onValueChange = { broadcastContent = it },
                        label = { Text("نص الرسالة المرسلة لجميع أعضاء الفئة *") },
                        placeholder = { Text("نص التوجيه...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = {
                            Icon(Icons.Default.EditNote, contentDescription = null, tint = GoldPrimary)
                        }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("رسالة عاجلة ذات أولوية قصوى", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("تمييز الرسالة كإشعار طارئ للأعضاء", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = isUrgent,
                            onCheckedChange = { isUrgent = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = GoldPrimary, checkedTrackColor = GoldDark)
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("نشر أيضاً كإعلان رئيسي في المنصة", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("تظهر في شريط الإعلانات الترويجية والإدارية", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = postAsAnnouncement,
                            onCheckedChange = { postAsAnnouncement = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = GoldPrimary, checkedTrackColor = GoldDark)
                        )
                    }
                    
                    if (lastSavedTime.isNotEmpty() && !isSending) {
                        Text(
                            text = lastSavedTime,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }

        // 4. Send Broadcast Action Button
        item {
            Button(
                onClick = {
                    if (broadcastContent.trim().isEmpty()) {
                        Toast.makeText(context, "يرجى كتابة نص الرسالة قبل الإرسال", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    isSending = true
                    onSendBroadcast(
                        selectedCategory,
                        broadcastTitle.trim(),
                        broadcastContent.trim(),
                        isUrgent,
                        postAsAnnouncement
                    ) { deliveredCount ->
                        isSending = false
                        lastSentCount = deliveredCount
                        broadcastTitle = ""
                        broadcastContent = ""
                        prefs.edit().remove("broadcast_title").remove("broadcast_content").apply()
                        lastSavedTime = ""
                        Toast.makeText(
                            context,
                            "تم إرسال الرسالة الجماعية بنجاح إلى $deliveredCount مستخدم",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                },
                enabled = !isSending && broadcastContent.trim().isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black)
            ) {
                if (isSending) {
                    CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("جاري إرسال البث الجماعي...", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                } else {
                    Icon(Icons.Default.Send, contentDescription = null, tint = Color.Black, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "إرسال البث الجماعي لفئة (${selectedCategory.label}) الآن",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }

        if (lastSentCount != null) {
            item {
                Surface(
                    color = Color(0xFF10B981).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981))
                        Text(
                            text = "تم بث وتوزيع آخر رسالة بنجاح إلى $lastSentCount محادثة ومستخدم",
                            color = Color(0xFF10B981),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TutorialsTab() {
    val context = LocalContext.current
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("إدارة الشروحات التعليمية", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = GoldPrimary)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        painter = painterResource(id = R.drawable.tutorials_empty_state),
                        contentDescription = "لا توجد شروحات",
                        modifier = Modifier
                            .size(180.dp)
                            .clip(RoundedCornerShape(16.dp))
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("لا توجد شروحات تعليمية حالياً", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("يمكنك إضافة فيديوهات أو مقالات تعليمية لمساعدة المهندسين والمستخدمين في فهم النظام.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            Toast.makeText(context, "إضافة الشروحات: يرجى وضع روابط الشرح في إعدادات المنصة المتقدمة", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("إضافة شرح جديد", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Diagnostic dashboard tab displaying database schema version integrity checks and real-time logs.
 */
@Composable
fun DatabaseIntegrityTab() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val reports by DatabaseIntegrityChecker.reports.collectAsStateWithLifecycle()
    val latestReport by DatabaseIntegrityChecker.latestReport.collectAsStateWithLifecycle()
    val isValidating by DatabaseIntegrityChecker.isValidating.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Header card with Run Integrity Check action
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
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
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(GoldPrimary.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Storage,
                                    contentDescription = null,
                                    tint = GoldPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "فحص سلامة وتكامل قاعدة البيانات",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "التحقق الدوري من توافق المخطط (Schema V19) وخلو الجداول من الأعطال",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    DatabaseIntegrityChecker.performIntegrityCheck(context)
                                }
                            },
                            enabled = !isValidating,
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            if (isValidating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.Black,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("جاري الفحص...", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("فحص فوري", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(14.dp))

                    // Stats Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("الإصدار المسجل", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("V${latestReport?.currentVersion ?: 19}", fontWeight = FontWeight.Black, fontSize = 15.sp, color = GoldPrimary)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("الإصدار المتوقع", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("V19 (Room)", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("الجداول النشطة", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${latestReport?.verifiedTablesCount ?: 35} جدول", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("الحالة الحالية", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            val statusText = when (latestReport?.status) {
                                IntegrityStatus.HEALTHY -> "سليمة 100%"
                                IntegrityStatus.WARNING -> "تحذير"
                                IntegrityStatus.CORRUPTED -> "تلف بيانات"
                                null -> "جاهزة للفحص"
                            }
                            val statusColor = when (latestReport?.status) {
                                IntegrityStatus.HEALTHY -> Color(0xFF10B981)
                                IntegrityStatus.WARNING -> Color(0xFFF59E0B)
                                IntegrityStatus.CORRUPTED -> Color(0xFFEF4444)
                                null -> GoldPrimary
                            }
                            Text(statusText, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = statusColor)
                        }
                    }
                }
            }
        }

        // Section Title: Logs List
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "سجل فحوصات المخطط والتكامل (${reports.size})",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "Debug Integrity Logs",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GoldPrimary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        if (reports.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier.padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "اضغط على زر 'فحص فوري' أعلاه لتنفيذ فحص تكامل قاعدة البيانات وتسجيل النتائج.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(reports, key = { "${it.timestamp}_${it.status}" }) { report ->
                val cardBorderColor = when (report.status) {
                    IntegrityStatus.HEALTHY -> Color(0xFF10B981).copy(alpha = 0.4f)
                    IntegrityStatus.WARNING -> Color(0xFFF59E0B).copy(alpha = 0.4f)
                    IntegrityStatus.CORRUPTED -> Color(0xFFEF4444).copy(alpha = 0.5f)
                }
                val badgeColor = when (report.status) {
                    IntegrityStatus.HEALTHY -> Color(0xFF10B981)
                    IntegrityStatus.WARNING -> Color(0xFFF59E0B)
                    IntegrityStatus.CORRUPTED -> Color(0xFFEF4444)
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, cardBorderColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(badgeColor)
                                )
                                Text(
                                    text = when (report.status) {
                                        IntegrityStatus.HEALTHY -> "فحص ناجح (PRAGMA quick_check OK)"
                                        IntegrityStatus.WARNING -> "تنبيه عدم تطابق الإصدار"
                                        IntegrityStatus.CORRUPTED -> "فشل فحص سلامة البيانات"
                                    },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = report.formattedTime,
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = report.integrityMessage,
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "مخطط: V${report.currentVersion} / المتوقع: V${report.expectedVersion}",
                                fontSize = 10.sp,
                                color = GoldPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "سجلات مسجلة: ~${report.totalRecordsCount}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
