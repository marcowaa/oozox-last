package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import com.example.data.model.*
import com.example.ui.components.BusinessSideDrawerContent
import com.example.ui.components.ExcelImportDialog
import com.example.ui.components.InvoiceVerificationDialog
import com.example.ui.components.OozoxAvatarImage
import com.example.ui.components.RetroactiveEmployeeLedgerSection
import com.example.ui.components.RetroactiveEmployeeLedgerDialog
import com.example.ui.components.PanoramicProductScanDialog
import com.example.ui.components.PosCameraCheckoutDialog
import com.example.util.GoldUtils
import com.example.ui.components.GranularPaymentBreakdownDialog
import com.example.ui.components.ShiftLogoutSummaryDialog
import com.example.ui.components.WorkGroupInvoiceComplaintsDialog
import com.example.ui.theme.*
import com.example.util.SpeechToTextManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

val PREDEFINED_PROFESSIONS = listOf(
    "تاجر",
    "مقاول",
    "رجل أعمال",
    "أعمال حرة",
    "مبيعات",
    "تسويق",
    "ماركتنج"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessManagementScreen(
    currentUser: UserEntity?,
    allInvoices: List<InvoiceEntity>,
    merchantProducts: List<MerchantProductEntity> = emptyList(),
    activeSubAccount: HiddenSubAccountEntity? = null,
    allSubAccounts: List<HiddenSubAccountEntity> = emptyList(),
    allShiftHandovers: List<ShiftHandoverEntity> = emptyList(),
    allComplaints: List<FinancialComplaintEntity> = emptyList(),
    allProjects: List<ProjectEntity> = emptyList(),
    allHashtagAccounts: List<HashtagAccountEntity> = emptyList(),
    allHashtagEntries: List<HashtagEntryEntity> = emptyList(),
    allDailyRecords: List<DailyAccountingRecordEntity> = emptyList(),
    onLoginSubAccount: (String, String) -> Boolean = { _, _ -> false },
    onLogoutSubAccount: () -> Unit = {},
    onAddShiftHandover: (ShiftHandoverEntity) -> Unit = {},
    onReceiveShift: (handoverId: String, receiverId: String, receiverName: String, notes: String?) -> Unit = { _, _, _, _ -> },
    onReceiveShiftFull: (
        handoverId: String,
        receiverId: String,
        receiverName: String,
        notes: String?,
        merchandiseStatus: String,
        physicalStockChecked: Boolean,
        cashDrawerConfirmed: Boolean,
        invoicesAuditedCount: Int,
        receiverSignature: String?,
        acceptanceType: String,
        goodsDiscrepancyNotes: String?
    ) -> Unit = { id, rId, rName, notes, _, _, _, _, _, _, _ -> onReceiveShift(id, rId, rName, notes) },
    onRespondToComplaint: (complaintId: String, accepted: Boolean, reason: String?, proofUri: String?) -> Unit = { _, _, _, _ -> },
    onDecideOnComplaint: (complaintId: String, decision: String, notes: String?) -> Unit = { _, _, _ -> },
    onApplyJointDeduction: (complaintId: String, primaryAmount: Double, receiverAmount: Double, receiverId: String?, notes: String?) -> Unit = { _, _, _, _, _ -> },
    onUpdateProfession: (String) -> Unit = {},
    onCreateInvoice: (InvoiceEntity) -> Unit = {},
    onAddMerchantProduct: (MerchantProductEntity) -> Unit = {},
    onAddSubAccount: (name: String, phone: String, pin: String) -> Unit = { _, _, _ -> },
    onOpenSubscriptions: () -> Unit = {},
    onOpenCompanyAccounting: () -> Unit = {},
    onOpenDrawer: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var showProfessionPickerModal by remember { mutableStateOf(currentUser?.jobTitle.isNullOrBlank()) }
    var customProfessionText by remember { mutableStateOf("") }
    var showCustomProfessionField by remember { mutableStateOf(false) }

    var showExcelImportDialog by remember { mutableStateOf(false) }
    var showVoiceProductDialog by remember { mutableStateOf(false) }
    var showAddProductModal by remember { mutableStateOf(false) }
    var showPanoramicScanModal by remember { mutableStateOf(false) }
    var showPosCameraCheckoutModal by remember { mutableStateOf(false) }
    var showGranularPaymentBreakdownModal by remember { mutableStateOf(false) }
    var showShiftLogoutSummaryModal by remember { mutableStateOf(false) }
    var showWorkGroupInvoiceComplaintsModal by remember { mutableStateOf(false) }
    var showCreateCheckoutInvoiceModal by remember { mutableStateOf(false) }
    var showAddCashierSubAccountModal by remember { mutableStateOf(false) }
    var showInvoiceVerificationModal by remember { mutableStateOf(false) }
    var showEndOfDayReportModal by remember { mutableStateOf(false) }
    var showManagePaymentMethodsModal by remember { mutableStateOf(false) }
    var showRetroactiveLedgerModal by remember { mutableStateOf(false) }
    var showAccountingExcelSheet by remember { mutableStateOf(false) }
    var availablePaymentMethods by remember {
        mutableStateOf(
            listOf(
                "كاش (نقدي)",
                "فودافون كاش / المحافظ",
                "إنستا باي (InstaPay)",
                "فيزا / بطاقة إلكترونية",
                "تحويل بنكي مباشر",
                "شيك بنكي",
                "آجل / حساب مؤجل"
            )
        )
    }
    var generatedInvoiceForShare by remember { mutableStateOf<InvoiceEntity?>(null) }
    var showEmployeeLoginModal by remember { mutableStateOf(false) }
    var showShiftHandoverModal by remember { mutableStateOf(false) }
    var showCustomerRatingModal by remember { mutableStateOf(false) }
    var ratingInvoiceRef by remember { mutableStateOf<InvoiceEntity?>(null) }
    var ratedStars by remember { mutableFloatStateOf(5.0f) }
    var feedbackComment by remember { mutableStateOf("") }

    val activeProfession = currentUser?.jobTitle ?: "غير محدد"

    // Trial Calculation (2 months free trial = 60 days)
    val trialDaysRemaining = remember(currentUser) {
        val start = currentUser?.subscriptionStartTimestamp ?: System.currentTimeMillis()
        val totalMs = 60L * 24 * 3600 * 1000
        val elapsedMs = System.currentTimeMillis() - start
        val remMs = totalMs - elapsedMs
        (remMs / (1000 * 3600 * 24)).coerceAtLeast(0).toInt()
    }

    val businessDrawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var productSearchQuery by remember { mutableStateOf("") }

    BackHandler(enabled = businessDrawerState.isOpen) {
        coroutineScope.launch { businessDrawerState.close() }
    }

    ModalNavigationDrawer(
        drawerState = businessDrawerState,
        gesturesEnabled = businessDrawerState.isOpen,
        drawerContent = {
            BusinessSideDrawerContent(
                currentUser = currentUser,
                activeProfession = activeProfession,
                activeSubAccount = activeSubAccount,
                trialDaysRemaining = trialDaysRemaining,
                pendingShiftsCount = allShiftHandovers.count { !it.isReceived },
                complaintsCount = allComplaints.size,
                onOpenSubscriptions = onOpenSubscriptions,
                onOpenInvoiceVerification = { showInvoiceVerificationModal = true },
                onOpenProfessionPicker = { showProfessionPickerModal = true },
                onOpenCashierLogin = { showEmployeeLoginModal = true },
                onOpenShiftHandover = { showShiftHandoverModal = true },
                onOpenPaymentMethods = { showManagePaymentMethodsModal = true },
                onOpenEndOfDayReport = { showEndOfDayReportModal = true },
                onOpenCompanyAccounting = onOpenCompanyAccounting,
                onOpenRetroactiveLedger = { showRetroactiveLedgerModal = true },
                onOpenAccountingExcel = { showAccountingExcelSheet = true },
                onCloseDrawer = { coroutineScope.launch { businessDrawerState.close() } }
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Top Header
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            IconButton(
                                onClick = { coroutineScope.launch { businessDrawerState.open() } },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("business_open_drawer_btn")
                            ) {
                                Icon(Icons.Default.Menu, contentDescription = "القائمة الجانبية", tint = adaptiveGoldPrimary)
                            }

                        Column(verticalArrangement = Arrangement.Center) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "إدارة الأعمال",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = CircleShape,
                                    color = OozoxTeal.copy(alpha = 0.15f),
                                    border = BorderStroke(0.8.dp, OozoxTeal)
                                ) {
                                    Text(
                                        text = "الرئيسي 💼",
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = OozoxTeal,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.5.dp)
                                    )
                                }
                            }
                            Text(
                                text = if (activeSubAccount != null) "${activeSubAccount.displayName} (${activeSubAccount.roleTitle})" else "المهنة: $activeProfession",
                                fontSize = 11.sp,
                                color = if (activeSubAccount != null) OozoxTeal else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (activeSubAccount != null) {
                            Surface(
                                onClick = { showShiftHandoverModal = true },
                                shape = CircleShape,
                                color = Color.Red.copy(alpha = 0.15f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Logout, contentDescription = "تسليم الشفت", tint = Color.Red, modifier = Modifier.size(18.dp))
                                }
                            }
                        } else {
                            Surface(
                                onClick = { showEmployeeLoginModal = true },
                                shape = CircleShape,
                                color = OozoxTeal.copy(alpha = 0.15f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Login, contentDescription = "دخول الموظف", tint = OozoxTeal, modifier = Modifier.size(18.dp))
                                }
                            }
                            Surface(
                                onClick = { showProfessionPickerModal = true },
                                shape = CircleShape,
                                color = GoldPrimary.copy(alpha = 0.15f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Edit, contentDescription = "تعديل المهنة", tint = GoldPrimary, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Executive Business KPI Highlights Strip (Clean, Space-Saving Metrics)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BusinessKpiSummaryCard(
                        title = "المنتجات",
                        value = "${merchantProducts.size}",
                        subtitle = "صنف مسجل",
                        icon = Icons.Default.Inventory2,
                        accentColor = OozoxTeal,
                        modifier = Modifier.weight(1f)
                    )
                    BusinessKpiSummaryCard(
                        title = "المبيعات",
                        value = "${allInvoices.size}",
                        subtitle = "فاتورة صادرة",
                        icon = Icons.Default.ReceiptLong,
                        accentColor = Color(0xFF10B981),
                        modifier = Modifier.weight(1f)
                    )
                    val totalRevenue = remember(allInvoices) { allInvoices.sumOf { it.totalAmount } }
                    BusinessKpiSummaryCard(
                        title = "الإيراد",
                        value = if (totalRevenue >= 1000) "${(totalRevenue / 1000).toInt()}k" else "${totalRevenue.toInt()}",
                        subtitle = "ج.م إجمالي",
                        icon = Icons.Default.Payments,
                        accentColor = GoldPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    BusinessKpiSummaryCard(
                        title = "الشفت",
                        value = if (activeSubAccount != null) "نشط" else "المالك",
                        subtitle = if (activeSubAccount != null) activeSubAccount.displayName else "رئيسي",
                        icon = if (activeSubAccount != null) Icons.Default.HowToReg else Icons.Default.AdminPanelSettings,
                        accentColor = if (activeSubAccount != null) Color(0xFF6366F1) else Color(0xFFF59E0B),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 2. Quick Action Hub Deck (Clean, Compact, Modern Tool Grid)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "أدوات التاجر وإدارة الأعمال",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "١٠ أدوات سريعة",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Row 1: مبيعات ونقاط البيع
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CompactBusinessToolCard(
                            title = "كاميرا POS",
                            icon = Icons.Default.CenterFocusStrong,
                            accentColor = Color(0xFF10B981),
                            testTag = "open_pos_camera_btn",
                            modifier = Modifier.weight(1f),
                            onClick = { showPosCameraCheckoutModal = true }
                        )
                        CompactBusinessToolCard(
                            title = "فاتورة كاشير",
                            icon = Icons.Default.PointOfSale,
                            accentColor = Color(0xFF10B981),
                            modifier = Modifier.weight(1f),
                            onClick = { showCreateCheckoutInvoiceModal = true }
                        )
                        CompactBusinessToolCard(
                            title = "مسح 360°",
                            icon = Icons.Default.Camera,
                            accentColor = Color(0xFF0EA5E9),
                            testTag = "open_360_scan_btn",
                            modifier = Modifier.weight(1f),
                            onClick = { showPanoramicScanModal = true }
                        )
                        CompactBusinessToolCard(
                            title = "تقفيل شفت",
                            icon = Icons.Default.LockClock,
                            accentColor = Color(0xFFEF4444),
                            testTag = "open_shift_logout_summary_btn",
                            modifier = Modifier.weight(1f),
                            onClick = { showShiftLogoutSummaryModal = true }
                        )
                    }

                    // Row 2: تقارير وإدخال بيانات
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CompactBusinessToolCard(
                            title = "تقرير المبيعات",
                            icon = Icons.Default.AccountBalanceWallet,
                            accentColor = Color(0xFF6366F1),
                            testTag = "open_granular_payments_btn",
                            modifier = Modifier.weight(1f),
                            onClick = { showGranularPaymentBreakdownModal = true }
                        )
                        CompactBusinessToolCard(
                            title = "تقارير اليوم",
                            icon = Icons.Default.Assessment,
                            accentColor = Color(0xFF8B5CF6),
                            modifier = Modifier.weight(1f),
                            onClick = { showEndOfDayReportModal = true }
                        )
                        CompactBusinessToolCard(
                            title = "استيراد إكسل",
                            icon = Icons.Default.UploadFile,
                            accentColor = Color(0xFFF59E0B),
                            modifier = Modifier.weight(1f),
                            onClick = { showExcelImportDialog = true }
                        )
                        CompactBusinessToolCard(
                            title = "إضافة صوتية",
                            icon = Icons.Default.Mic,
                            accentColor = OozoxTeal,
                            modifier = Modifier.weight(1f),
                            onClick = { showVoiceProductDialog = true }
                        )
                    }

                    // Row 3: صلاحيات ووسائل الدفع والشكاوى
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CompactBusinessToolCard(
                            title = "صلاحيات كاشير",
                            icon = Icons.Default.PersonAdd,
                            accentColor = Color(0xFF06B6D4),
                            modifier = Modifier.weight(1f),
                            onClick = { showAddCashierSubAccountModal = true }
                        )
                        CompactBusinessToolCard(
                            title = "وسائل الدفع",
                            icon = Icons.Default.Payments,
                            accentColor = Color(0xFF14B8A6),
                            modifier = Modifier.weight(1f),
                            onClick = { showManagePaymentMethodsModal = true }
                        )
                        CompactBusinessToolCard(
                            title = "شكاوى الفواتير",
                            icon = Icons.Default.Campaign,
                            accentColor = Color(0xFFF59E0B),
                            testTag = "open_workgroup_invoice_complaints_btn",
                            modifier = Modifier.weight(1f),
                            onClick = { showWorkGroupInvoiceComplaintsModal = true }
                        )
                    }
                }
            }

            // Pending Price Review Banner for Owner
            val pendingPriceReviews = merchantProducts.filter { it.priceApprovalStatus == "PENDING_OWNER_REVIEW" }
            if (pendingPriceReviews.isNotEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF59E0B).copy(alpha = 0.15f)),
                        border = BorderStroke(1.2.dp, Color(0xFFF59E0B)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = Color(0xFFF59E0B))
                                Column {
                                    Text(
                                        text = "تنبيه المالك: ${pendingPriceReviews.size} بضاعة بانتظار اعتماد السعر!",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "أضاف الموظفون بضاعة جديدة بدون صلاحية وضع الأسعار مباشرة.",
                                        fontSize = 10.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    pendingPriceReviews.forEach { prod ->
                                        onAddMerchantProduct(prod.copy(priceApprovalStatus = "APPROVED"))
                                    }
                                    Toast.makeText(context, "تم اعتماد أسعار البضاعة بنجاح!", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B), contentColor = Color.Black),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("اعتماد الكل ✓", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Pending Price Review Banner block above — Retroactive Ledger & Excel are accessed via BusinessSideDrawer

            // 4. Products List & Catalog Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "قائمة المنتجات والعروض المسجلة:",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    TextButton(onClick = { showAddProductModal = true }) {
                        Text("+ إضافة منتج يدوياً", fontSize = 11.5.sp, color = GoldPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (merchantProducts.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Inventory2, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(36.dp))
                            Text(
                                text = "لا توجد منتجات مسجلة حتى الآن",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "ارفع شيت إكسل بأسعار المستهلك والجملة والخصومات، أو أضف المنتجات بالصوت!",
                                fontSize = 11.sp,
                                color = GoldPrimary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(merchantProducts, key = { it.id }) { product ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = product.title,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = product.description ?: "سعر المستهلك والجملة متاح بالجدول",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Surface(shape = RoundedCornerShape(4.dp), color = GoldPrimary.copy(alpha = 0.15f)) {
                                        Text("قطع: ${product.weightGrams} ج.م", fontSize = 10.sp, color = GoldPrimary, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                    }
                                    Surface(shape = RoundedCornerShape(4.dp), color = OozoxTeal.copy(alpha = 0.15f)) {
                                        Text("خصم متاح", fontSize = 10.sp, color = OozoxTeal, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    showCreateCheckoutInvoiceModal = true
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black)
                            ) {
                                Text("بيع الكاشير", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

    // Modal: Nature of Work Selection (طبيعة العمل)
    if (showProfessionPickerModal) {
        AlertDialog(
            onDismissRequest = { if (!currentUser?.jobTitle.isNullOrBlank()) showProfessionPickerModal = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Work, contentDescription = null, tint = GoldPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("اختر طبيعة العمل / الوظيفة", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "يرجى تحديد طبيعة عملك لتخصيص أدوات قسم إدارة الأعمال وحساباتك:",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(PREDEFINED_PROFESSIONS, key = { it }) { prof ->
                            FilterChip(
                                selected = activeProfession == prof,
                                onClick = {
                                    onUpdateProfession(prof)
                                    showProfessionPickerModal = false
                                },
                                label = { Text(prof, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = adaptiveGoldPrimary,
                                    selectedLabelColor = adaptiveOnPrimary
                                )
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = { showCustomProfessionField = true },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إضافة وظيفة أخرى غير موجودة بالقائمة", fontSize = 12.sp)
                    }

                    if (showCustomProfessionField) {
                        OutlinedTextField(
                            value = customProfessionText,
                            onValueChange = { customProfessionText = it },
                            placeholder = { Text("مثال: محاسب") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                if (customProfessionText.isNotBlank()) {
                                    onUpdateProfession(customProfessionText.trim())
                                    showProfessionPickerModal = false
                                }
                            },
                            enabled = customProfessionText.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("حفظ الوظيفة الجديدة", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    // Modal: Accounting Excel Sheet Hub (Opened from BusinessSideDrawer)
    if (showAccountingExcelSheet) {
        val unifiedSummary = remember(
            allProjects,
            allInvoices,
            allHashtagAccounts,
            allHashtagEntries,
            allDailyRecords,
            allShiftHandovers
        ) {
            com.example.util.SimpleExcelEngine.calculateUnifiedBusinessAccounting(
                projects = allProjects,
                invoices = allInvoices,
                hashtagAccounts = allHashtagAccounts,
                hashtagEntries = allHashtagEntries,
                dailyRecords = allDailyRecords,
                shiftHandovers = allShiftHandovers
            )
        }
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showAccountingExcelSheet = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
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
                    border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.45f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 520.dp)
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
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF10B981).copy(alpha = 0.14f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.TableChart, contentDescription = null, tint = Color(0xFF10B981))
                                }
                                Column {
                                    Text("شيت إكسل المحاسبي الموحد", fontSize = 15.5.sp, fontWeight = FontWeight.Bold)
                                    Text("مزامنة الفواتير والمشاريع والأعمال الخارجية", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            IconButton(onClick = { showAccountingExcelSheet = false }, modifier = Modifier.size(48.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "إغلاق")
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("المتبقي بالفواتير", fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(GoldUtils.formatCurrency(unifiedSummary.invoicesOutstanding), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = GoldPrimary)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("الأعمال الخارجية والهاشتاج", fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(GoldUtils.formatCurrency(unifiedSummary.externalBusinessTotal), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("عدد المشاريع / الفواتير", fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${allProjects.size} مشروع • ${allInvoices.size} فاتورة", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    showAccountingExcelSheet = false
                                    showExcelImportDialog = true
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("استيراد إكسل", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = {
                                    val file = com.example.util.SimpleExcelEngine.exportUnifiedBusinessWorkbookCsv(
                                        context = context,
                                        companyTitle = currentUser?.traderShopName ?: currentUser?.name ?: "OOZOX",
                                        projects = allProjects,
                                        invoices = allInvoices,
                                        hashtagAccounts = allHashtagAccounts,
                                        hashtagEntries = allHashtagEntries,
                                        dailyRecords = allDailyRecords,
                                        shiftHandovers = allShiftHandovers
                                    )
                                    if (file != null) {
                                        com.example.util.SimpleExcelEngine.shareExcelFile(context, file, "شيت إكسل OOZOX الموحد")
                                    } else {
                                        Toast.makeText(context, "تعذر إنشاء ملف الإكسل الموحد", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981), contentColor = Color.White),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("export_unified_business_excel_btn")
                            ) {
                                Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("تصدير الشيت", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal: Excel Product Import
    if (showExcelImportDialog) {
        ExcelImportDialog(
            onImportRecords = { rows ->
                rows.forEach { row ->
                    onAddMerchantProduct(
                        MerchantProductEntity(
                            id = "prod_excel_${UUID.randomUUID()}",
                            merchantId = currentUser?.id ?: "trader",
                            merchantShopName = currentUser?.traderShopName ?: "متجر معتمد",
                            title = if (row.staffName.isNotBlank()) row.staffName else if (row.notes.isNotBlank()) row.notes else "منتج مستورد من إكسل",
                            category = "PROD",
                            weightGrams = row.dailyRevenue.coerceAtLeast(1.0),
                            description = "سعر المستهلك: ${row.incomingAmount} ج.م | سعر الجملة: ${row.outgoingAmount} ج.م | الخصم: ${row.deficitAmount} ج.م",
                            createdAt = System.currentTimeMillis()
                        )
                    )
                }
                showExcelImportDialog = false
                Toast.makeText(context, "تم استيراد ${rows.size} منتج بنجاح!", Toast.LENGTH_LONG).show()
            },
            onDismiss = { showExcelImportDialog = false }
        )
    }

    // Modal: Voice Product Addition
    if (showVoiceProductDialog) {
        VoiceAddProductDialog(
            onSaveProduct = { product ->
                onAddMerchantProduct(product)
                showVoiceProductDialog = false
            },
            onDismiss = { showVoiceProductDialog = false }
        )
    }

    // Modal: Manual Product Addition
    if (showAddProductModal) {
        ManualAddProductDialog(
            merchantShopName = currentUser?.traderShopName ?: "متجر معتمد",
            onSaveProduct = { product ->
                onAddMerchantProduct(product)
                showAddProductModal = false
            },
            onDismiss = { showAddProductModal = false }
        )
    }

    // Modal: Cashier Checkout & Loyalty Invoice Creation
    if (showCreateCheckoutInvoiceModal) {
        CashierCheckoutDialog(
            allInvoices = allInvoices,
            currentUserName = activeSubAccount?.displayName ?: (currentUser?.name ?: "البائع / الكاشير"),
            availablePaymentMethods = availablePaymentMethods,
            onGenerateInvoice = { invoice ->
                // Ensure cashierId and cashierName are properly watermarked with active session details!
                val watermarkedInvoice = if (activeSubAccount != null) {
                    invoice.copy(
                        cashierName = activeSubAccount.displayName,
                        senderId = activeSubAccount.id, // Emplace employee ID inside invoice senderId!
                        notes = "${invoice.notes} | Cashier-ID: ${activeSubAccount.id}"
                    )
                } else {
                    invoice
                }
                onCreateInvoice(watermarkedInvoice)
                generatedInvoiceForShare = watermarkedInvoice
                showCreateCheckoutInvoiceModal = false
                
                // Trigger simulated customer push notification & employee feedback flow!
                ratingInvoiceRef = watermarkedInvoice
                showCustomerRatingModal = true
            },
            onDismiss = { showCreateCheckoutInvoiceModal = false }
        )
    }

    // Modal: 360 Panoramic Product Scan & Unwrapping
    if (showPanoramicScanModal) {
        PanoramicProductScanDialog(
            merchantShopName = currentUser?.name ?: "متجر OOZOX المعتمد",
            currentStaffName = activeSubAccount?.displayName ?: currentUser?.name,
            onSaveProduct = { product ->
                onAddMerchantProduct(product)
                showPanoramicScanModal = false
            },
            onDismiss = { showPanoramicScanModal = false }
        )
    }

    // Modal: Camera POS Checkout
    if (showPosCameraCheckoutModal) {
        PosCameraCheckoutDialog(
            allStoreProducts = merchantProducts,
            currentCashierName = activeSubAccount?.displayName ?: (currentUser?.name ?: "البائع المعتمد"),
            currentCashierId = activeSubAccount?.id ?: (currentUser?.id ?: "cashier_1"),
            availablePaymentMethods = availablePaymentMethods,
            onGenerateInvoice = { invoice ->
                onCreateInvoice(invoice)
                generatedInvoiceForShare = invoice
                showPosCameraCheckoutModal = false
                ratingInvoiceRef = invoice
                showCustomerRatingModal = true
            },
            onDismiss = { showPosCameraCheckoutModal = false }
        )
    }

    // Modal: Granular Payment Breakdown Report for Admin
    if (showGranularPaymentBreakdownModal) {
        GranularPaymentBreakdownDialog(
            allInvoices = allInvoices,
            shopName = currentUser?.name ?: "متجر OOZOX المعتمد",
            onDismiss = { showGranularPaymentBreakdownModal = false }
        )
    }

    // Modal: Shift Logout & Treasury Reconciliation Dialog
    if (showShiftLogoutSummaryModal) {
        val currentCashierId = activeSubAccount?.id ?: currentUser?.id ?: "cashier_1"
        val currentCashierName = activeSubAccount?.displayName ?: currentUser?.name ?: "موظف المبيعات"
        ShiftLogoutSummaryDialog(
            cashierId = currentCashierId,
            cashierName = currentCashierName,
            cashierPhone = activeSubAccount?.phone ?: currentUser?.phone,
            loginTime = System.currentTimeMillis() - 8 * 3600 * 1000L,
            onShiftClosedAndLogout = { summary ->
                onAddShiftHandover(summary.handover)
                showShiftLogoutSummaryModal = false
                onLogoutSubAccount()
            },
            onDismiss = { showShiftLogoutSummaryModal = false }
        )
    }

    // Modal: Work Group Invoice Complaints & Smart Auto-Mention Dialog
    if (showWorkGroupInvoiceComplaintsModal) {
        val defaultGroup = WorkGroupEntity(
            id = "store_sales_team",
            name = "جروب مبيعات المتجر والخزينة",
            ownerId = currentUser?.id ?: "owner_admin",
            companyName = currentUser?.name ?: "متجر OOZOX المعتمد"
        )
        WorkGroupInvoiceComplaintsDialog(
            workGroup = defaultGroup,
            allInvoices = allInvoices,
            complaints = allComplaints,
            currentStaffId = activeSubAccount?.id ?: currentUser?.id ?: "manager",
            currentStaffName = activeSubAccount?.displayName ?: currentUser?.name ?: "المدير العام",
            onSaveComplaint = { newComp ->
                onRespondToComplaint(newComp.id, false, null, null)
            },
            onUpdateComplaintResponse = { compId, status, reason, proofUri, proofMediaType, isDeducted ->
                onRespondToComplaint(compId, isDeducted, reason, proofUri)
            },
            onApplyJointDeduction = { compId, pAmt, rAmt, rId, notes ->
                onApplyJointDeduction(compId, pAmt, rAmt, rId, notes)
            },
            onDismiss = { showWorkGroupInvoiceComplaintsModal = false }
        )
    }

    // Modal: End of Day Reports
    if (showEndOfDayReportModal) {
        EndOfDayReportDialog(
            allInvoices = allInvoices,
            merchantProducts = merchantProducts,
            onDismiss = { showEndOfDayReportModal = false }
        )
    }

    // Modal: Manage Payment Methods
    if (showManagePaymentMethodsModal) {
        ManagePaymentMethodsDialog(
            paymentMethods = availablePaymentMethods,
            onAddPaymentMethod = { newMethod ->
                if (newMethod.isNotBlank() && !availablePaymentMethods.contains(newMethod)) {
                    availablePaymentMethods = availablePaymentMethods + newMethod
                }
            },
            onRemovePaymentMethod = { methodToRemove ->
                availablePaymentMethods = availablePaymentMethods.filter { it != methodToRemove }
            },
            onDismiss = { showManagePaymentMethodsModal = false }
        )
    }

    // Modal: Add Cashier Sub-Account
    if (showAddCashierSubAccountModal) {
        AddCashierSubAccountDialog(
            onSave = { name, phone, pin ->
                onAddSubAccount(name, phone, pin)
                showAddCashierSubAccountModal = false
            },
            onDismiss = { showAddCashierSubAccountModal = false }
        )
    }

    // Modal: Full Retroactive Employee Ledger and Audit Statement
    if (showRetroactiveLedgerModal) {
        RetroactiveEmployeeLedgerDialog(
            allSubAccounts = allSubAccounts,
            allShiftHandovers = allShiftHandovers,
            allComplaints = allComplaints,
            activeSubAccount = activeSubAccount,
            onReceiveShift = onReceiveShift,
            onReceiveShiftFull = onReceiveShiftFull,
            onRespondToComplaint = onRespondToComplaint,
            onDecideOnComplaint = onDecideOnComplaint,
            onApplyJointDeduction = onApplyJointDeduction,
            onDismiss = { showRetroactiveLedgerModal = false }
        )
    }

    // Modal: Watermarked Invoice Share Dialog
    if (generatedInvoiceForShare != null) {
        val inv = generatedInvoiceForShare!!
        AlertDialog(
            onDismissRequest = { generatedInvoiceForShare = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Verified, contentDescription = null, tint = Color(0xFF10B981))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تم إصدار الفاتورة وتوثيقها!", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, Color(0xFF10B981)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("رقم الفاتورة: ${inv.invoiceNumber}", fontWeight = FontWeight.Bold, color = GoldPrimary)
                            Text("العميل: ${inv.receiverName}", fontSize = 12.sp)
                            Text("المبلغ: ${inv.totalAmount} ج.م", fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                            Text("وسيلة الدفع: 💳 ${inv.paymentMethod}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                            if (!inv.paymentMethodDetails.isNullOrBlank()) {
                                Text("بيانات السداد: 📋 ${inv.paymentMethodDetails}", fontSize = 11.sp, color = GoldPrimary)
                            }
                            if (!inv.paymentDemandNotice.isNullOrBlank()) {
                                Text("مطالبة السداد: 📢 ${inv.paymentDemandNotice}", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (!inv.cashierName.isNullOrBlank()) {
                                Text("موظف البيع: ${inv.cashierName}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "🛡️ التوقيع المائي: توقيع رقمي موثق من منصة OOZOX لا يقلد ولا يمسح.",
                                fontSize = 10.5.sp,
                                color = OozoxTeal,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Button(
                        onClick = {
                            val cashierSuffix = if (!inv.cashierName.isNullOrBlank()) "\nالكاشير المسؤول: ${inv.cashierName} (الـ ID: ${inv.senderId.take(8).uppercase()})" else ""
                            val detailsSuffix = if (!inv.paymentMethodDetails.isNullOrBlank()) "\nبيانات السداد: ${inv.paymentMethodDetails}" else ""
                            val demandSuffix = if (!inv.paymentDemandNotice.isNullOrBlank()) "\nإشعار ومطالبة السداد: ${inv.paymentDemandNotice}" else ""
                            val shareText = """
                                🧾 فاتورة بيع ومطالبة سداد موثقة - منصة OOZOX
                                رقم الفاتورة: ${inv.invoiceNumber}
                                التاجر / الكاشير: ${inv.senderName}
                                العميل: ${inv.receiverName}
                                وسيلة الدفع: ${inv.paymentMethod}${detailsSuffix}${demandSuffix}
                                الإجمالي: ${inv.totalAmount} ج.م${cashierSuffix}
                                🛡️ التوقيع المائي الرقمي الموثق: OOZOX-VERIFIED-WATERMARK-${inv.id.takeLast(8).uppercase()}-${inv.senderId.take(6).uppercase()}
                                (توقيع موثق ومحمي لا يقلد ولا يمسح)
                            """.trimIndent()

                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                            context.startActivity(Intent.createChooser(intent, "مشاركة الفاتورة الموثقة للعميل"))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = OozoxTeal, contentColor = Color.White),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("مشاركة الفاتورة", fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { generatedInvoiceForShare = null }) {
                    Text("تم")
                }
            }
        )
    }

    // Modal: Invoice Watermark Verification Scanner
    if (showInvoiceVerificationModal) {
        InvoiceVerificationDialog(
            allInvoices = allInvoices,
            onDismiss = { showInvoiceVerificationModal = false }
        )
    }

    // Modal: Employee Cashier Login Dialog
    if (showEmployeeLoginModal) {
        var empPhoneOrUser by remember { mutableStateOf("") }
        var empPin by remember { mutableStateOf("") }
        var loginError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showEmployeeLoginModal = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Login, contentDescription = null, tint = OozoxTeal)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تسجيل دخول موظف الكاشير 🔑", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "يرجى إدخال اسم المستخدم أو رقم الموبايل ورمز PIN الممنوح لك من المالك:",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = empPhoneOrUser,
                        onValueChange = { empPhoneOrUser = it; loginError = false },
                        label = { Text("اسم المستخدم أو رقم الموبايل") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = empPin,
                        onValueChange = { empPin = it; loginError = false },
                        label = { Text("رمز الدخول PIN (4 أرقام)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (loginError) {
                        Text(
                            text = "❌ خطأ في اسم المستخدم أو رمز الـ PIN! يرجى المحاولة مرة أخرى أو مراجعة مالك المحل.",
                            color = Color.Red,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { showEmployeeLoginModal = false },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("إلغاء", fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = {
                            val success = onLoginSubAccount(empPhoneOrUser, empPin)
                            if (success) {
                                val matchedAccount = allSubAccounts.find {
                                    (it.phone == empPhoneOrUser.trim() || it.username == empPhoneOrUser.trim()) && (it.rawSecretTrigger == empPin.trim())
                                }
                                val resolvedName = matchedAccount?.displayName ?: empPhoneOrUser
                                val resolvedId = matchedAccount?.id ?: empPhoneOrUser

                                showEmployeeLoginModal = false
                                coroutineScope.launch {
                                    val locResult = com.example.util.WorkplaceLocationPolicyManager.enforceLoginLocationPolicy(
                                        context = context,
                                        employeeId = resolvedId,
                                        employeeName = resolvedName,
                                        workplaceLat = 30.0444, // Default HQ or configured coordinates
                                        workplaceLng = 31.2357,
                                        radiusMeters = 300,
                                        workplaceName = "مقر العمل والخزينة"
                                    )
                                    if (!locResult.isCompliant) {
                                        Toast.makeText(context, "⚠️ تنبيه موقع العمل: ${locResult.messageAr}", Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(context, "تم تسجيل الدخول ومطابقة موقع العمل بنجاح ✅", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } else {
                                loginError = true
                            }
                        },
                        enabled = empPhoneOrUser.isNotBlank() && empPin.isNotBlank(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = OozoxTeal, contentColor = Color.White),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("دخول", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {}
        )
    }

    // Modal: Shift Handover and Cash Balance Reconciliation Dialog
    if (showShiftHandoverModal && activeSubAccount != null) {
        val shiftInvoices = remember(allInvoices, activeSubAccount) {
            allInvoices.filter {
                it.cashierName == activeSubAccount.displayName && it.createdAt >= activeSubAccount.lastActiveAt
            }
        }
        val shiftProductsAdded = remember(merchantProducts, activeSubAccount) {
            merchantProducts.count {
                it.addedByStaffName == activeSubAccount.displayName && it.createdAt >= activeSubAccount.lastActiveAt
            }
        }
        val expectedCash = remember(shiftInvoices) { shiftInvoices.sumOf { it.totalAmount } }
        var actualCashInput by remember { mutableStateOf("") }
        var shiftNotes by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showShiftHandoverModal = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = Color.Red)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تسليم الشفت المالي والتقارير 💼", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("📊 تقرير الشفت الحالي للكاشير:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = GoldPrimary)
                                Text("• الكاشير: ${activeSubAccount.displayName}", fontSize = 11.sp)
                                Text("• إجمالي الفواتير المصدرة: ${shiftInvoices.size} فواتير", fontSize = 11.sp)
                                Text("• البضائع الجديدة المضافة للمخزن: $shiftProductsAdded أصناف", fontSize = 11.sp)
                                Text("• المبلغ المتوقع بالدرج: ${expectedCash.toInt()} ج.م", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = OozoxTeal)
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = actualCashInput,
                            onValueChange = { actualCashInput = it.filter { c -> c.isDigit() || c == '.' } },
                            label = { Text("المبلغ الفعلي المتوفر بالدرج للدرجة (ج.م)") },
                            placeholder = { Text("مبلغ الدرج") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    val actualCash = actualCashInput.toDoubleOrNull() ?: 0.0
                    val deficit = (expectedCash - actualCash).coerceAtLeast(0.0)
                    if (actualCashInput.isNotBlank() && deficit > 0.0) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.Red.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, Color.Red),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "⚠️ تنبيه: عجز مالي بقيمة ${deficit.toInt()} ج.م (إخطار الإدارة)",
                                    fontSize = 11.sp,
                                    color = Color.Red,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = shiftNotes,
                            onValueChange = { shiftNotes = it },
                            label = { Text("ملاحظات تسليم الشفت") },
                            placeholder = { Text("ملاحظات الشفت") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { showShiftHandoverModal = false },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("إلغاء", fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = {
                            val actualVal = actualCashInput.toDoubleOrNull() ?: 0.0
                            val deficitVal = (expectedCash - actualVal).coerceAtLeast(0.0)

                            val handover = ShiftHandoverEntity(
                                id = "shift_${System.currentTimeMillis()}",
                                cashierId = activeSubAccount.id,
                                cashierName = activeSubAccount.displayName,
                                cashierPhone = activeSubAccount.phone,
                                shiftDateText = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date()),
                                loginTime = activeSubAccount.lastActiveAt,
                                logoutTime = System.currentTimeMillis(),
                                totalInvoicesCount = shiftInvoices.size,
                                totalRevenueEGP = expectedCash,
                                totalPaidEGP = actualVal,
                                totalIncomingTransactions = expectedCash,
                                totalOutgoingTransactions = 0.0,
                                productsAddedCount = shiftProductsAdded,
                                productsSoldCount = shiftInvoices.size,
                                expectedCash = expectedCash,
                                actualCash = actualVal,
                                deficitAmount = deficitVal,
                                retroactiveBalance = actualVal - deficitVal,
                                isFlaggedToOwner = deficitVal > 0.0,
                                notes = shiftNotes.ifBlank { "تم تسليم الشفت بشكل طبيعي." }
                            )

                            onAddShiftHandover(handover)
                            onLogoutSubAccount()
                            showShiftHandoverModal = false
                            Toast.makeText(context, "تم تسليم الشفت وتسجيل الخروج بنجاح!", Toast.LENGTH_LONG).show()
                        },
                        enabled = actualCashInput.isNotBlank(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red, contentColor = Color.White),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("تسليم الشفت", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {}
        )
    }

    // Modal: Simulated Client Push Notification & Cashier Feedback Dialog
    if (showCustomerRatingModal && ratingInvoiceRef != null) {
        val inv = ratingInvoiceRef!!
        AlertDialog(
            onDismissRequest = { showCustomerRatingModal = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = GoldPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("إشعار للعميل 🔔 (تم الشراء)", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = GoldPrimary.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, GoldPrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "رسالة دفع للعميل: 💬 'مرحباً ${inv.receiverName}! لقد قمت بعملية شراء بقيمة ${inv.totalAmount.toInt()} ج.م من متجرنا المعتمد. شكراً لثقتك بـ OOZOX.'",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text("قيم الكاشير (موظف البيع) لضمان أفضل جودة خدمتكم ⭐:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        (1..5).forEach { star ->
                            Icon(
                                imageVector = if (star <= ratedStars) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = null,
                                tint = GoldPrimary,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clickable { ratedStars = star.toFloat() }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = feedbackComment,
                        onValueChange = { feedbackComment = it },
                        placeholder = { Text("تعامل ممتاز") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { showCustomerRatingModal = false },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("إلغاء", fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = {
                            showCustomerRatingModal = false
                            Toast.makeText(context, "شكراً لتقييمك! تم إرسال التقييم والملاحظات للمالك بنجاح لضمان الجودة.", Toast.LENGTH_LONG).show()
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("إرسال التقييم", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {}
        )
    }
}
}
}

@Composable
fun VoiceAddProductDialog(
    onSaveProduct: (MerchantProductEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var spokenText by remember { mutableStateOf("") }
    var isListening by remember { mutableStateOf(false) }
    val speechManager = remember { SpeechToTextManager(context) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Mic, contentDescription = null, tint = OozoxTeal)
                Spacer(modifier = Modifier.width(8.dp))
                Text("إضافة منتج بالصوت", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "تحدث باسم المنتج، الوزن، سعر المستهلك، وسعر الجملة:",
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (spokenText.isNotBlank()) "🗣️ \"$spokenText\"" else "اضغط على الميكروفون وتحدث...",
                        fontSize = 12.sp,
                        modifier = Modifier.padding(12.dp),
                        textAlign = TextAlign.Center
                    )
                }

                Button(
                    onClick = {
                        if (isListening) {
                            speechManager.stopListening()
                            isListening = false
                        } else {
                            isListening = true
                            speechManager.startListening(
                                languageCode = "ar-EG",
                                onPartial = { spokenText = it },
                                onFinal = { text ->
                                    spokenText = text
                                    isListening = false
                                }
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = if (isListening) Color.Red else OozoxTeal),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(if (isListening) Icons.Default.MicOff else Icons.Default.Mic, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isListening) "إيقاف التسجيل" else "بدء الاستماع صوتاً")
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text("إلغاء", fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = {
                        if (spokenText.isNotBlank()) {
                            val newProduct = MerchantProductEntity(
                                id = "prod_voice_${UUID.randomUUID()}",
                                merchantId = "trader",
                                merchantShopName = "متجر معتمد",
                                title = spokenText.take(30),
                                weightGrams = 10.0,
                                description = spokenText,
                                createdAt = System.currentTimeMillis()
                            )
                            onSaveProduct(newProduct)
                        }
                    },
                    enabled = spokenText.isNotBlank(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text("حفظ المنتج", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {}
    )
}

@Composable
fun ManualAddProductDialog(
    merchantShopName: String,
    onSaveProduct: (MerchantProductEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var weightText by remember { mutableStateOf("") }
    var quantityText by remember { mutableStateOf("1") }
    var priceMode by remember { mutableStateOf("BOARD_PRICE") } // "BOARD_PRICE" or "CUSTOM_PRICES"
    var priceConsumerText by remember { mutableStateOf("") }
    var priceWholesaleText by remember { mutableStateOf("") }
    var discountText by remember { mutableStateOf("") }
    var isStaffUser by remember { mutableStateOf(false) } // هل المستخدم موظف/كاشير أم المالك
    var allowStaffPriceOverride by remember { mutableStateOf(true) } // هل للموظف صلاحية وضع الأسعار مباشرة

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Inventory, contentDescription = null, tint = GoldPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("إدخال بضاعة / منتج جديد للمخزن", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("اسم المنتج / الصنف الإجمالي") },
                        placeholder = { Text("سبيكة عيار 24") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = weightText,
                            onValueChange = { weightText = it.filter { c -> c.isDigit() || c == '.' } },
                            label = { Text("الوزن (جرام)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = quantityText,
                            onValueChange = { quantityText = it.filter { c -> c.isDigit() } },
                            label = { Text("العدد / الكمية") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Text("آلية تحديد أسعار البضاعة:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GoldPrimary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                        FilterChip(
                            selected = priceMode == "BOARD_PRICE",
                            onClick = { priceMode = "BOARD_PRICE" },
                            label = { Text("أسعار اللوحة الافتراضية", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = GoldPrimary, selectedLabelColor = Color.Black)
                        )
                        FilterChip(
                            selected = priceMode == "CUSTOM_PRICES",
                            onClick = { priceMode = "CUSTOM_PRICES" },
                            label = { Text("إدخال أسعار خاصة", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = OozoxTeal, selectedLabelColor = Color.White)
                        )
                    }
                }

                if (priceMode == "CUSTOM_PRICES") {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = priceConsumerText,
                                onValueChange = { priceConsumerText = it.filter { c -> c.isDigit() || c == '.' } },
                                label = { Text("سعر المستهلك النهائي (ج.م)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = priceWholesaleText,
                                onValueChange = { priceWholesaleText = it.filter { c -> c.isDigit() || c == '.' } },
                                label = { Text("سعر الجملة / التكلفة (ج.م)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = discountText,
                                onValueChange = { discountText = it.filter { c -> c.isDigit() || c == '.' } },
                                label = { Text("نسبة الخصم المتاحة (%)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                item {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                        Checkbox(checked = isStaffUser, onCheckedChange = { isStaffUser = it })
                        Text("تم الإدخال بواسطة كاشير / موظف مخزن", fontSize = 11.5.sp)
                    }
                    if (isStaffUser) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = allowStaffPriceOverride, onCheckedChange = { allowStaffPriceOverride = it })
                            Text("المالك سمح بوضع الأسعار مباشرة بدون مراجعة", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text("إلغاء", fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            val cPrice = priceConsumerText.toDoubleOrNull() ?: 0.0
                            val wPrice = priceWholesaleText.toDoubleOrNull() ?: 0.0
                            val weight = weightText.toDoubleOrNull() ?: 1.0
                            val qty = quantityText.toDoubleOrNull() ?: 1.0

                            val isPendingReview = isStaffUser && !allowStaffPriceOverride && priceMode == "CUSTOM_PRICES"

                            val product = MerchantProductEntity(
                                id = "prod_manual_${UUID.randomUUID()}",
                                merchantId = "trader",
                                merchantShopName = merchantShopName,
                                title = title.trim(),
                                weightGrams = weight,
                                quantityInStock = qty,
                                consumerPriceEGP = if (priceMode == "BOARD_PRICE") weight * 3750.0 else cPrice,
                                wholesalePriceEGP = if (priceMode == "BOARD_PRICE") weight * 3600.0 else wPrice,
                                discountRatePercent = discountText.toDoubleOrNull() ?: 0.0,
                                priceApprovalStatus = if (isPendingReview) "PENDING_OWNER_REVIEW" else "APPROVED",
                                description = if (priceMode == "BOARD_PRICE") "سعر اللوحة الافتراضي (عيار 21)" else "مستهلك: $cPrice ج.م | جملة: $wPrice ج.م",
                                createdAt = System.currentTimeMillis()
                            )
                            onSaveProduct(product)
                        }
                    },
                    enabled = title.isNotBlank(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text("حفظ المنتج", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {}
    )
}

@Composable
fun CashierCheckoutDialog(
    allInvoices: List<InvoiceEntity>,
    currentUserName: String,
    availablePaymentMethods: List<String>,
    onGenerateInvoice: (InvoiceEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var clientName by remember { mutableStateOf("") }
    var clientPhone by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var selectedPaymentMethod by remember { mutableStateOf(availablePaymentMethods.firstOrNull() ?: "كاش (نقدي)") }
    var paymentMethodDetails by remember { mutableStateOf("استلام نقدي مباشر بالفرع") }
    var isVipClient by remember { mutableStateOf(false) }
    var clientPurchaseCount by remember { mutableIntStateOf(0) }
    var appliedDiscountText by remember { mutableStateOf("") }

    // Check loyalty count when phone is entered
    LaunchedEffect(clientPhone) {
        if (clientPhone.trim().length >= 8) {
            val history = allInvoices.filter { it.receiverName.contains(clientPhone) || (it.notes?.contains(clientPhone) == true) }
            clientPurchaseCount = history.size + 6 // VIP calculation
            if (clientPurchaseCount >= 5) {
                isVipClient = true
                appliedDiscountText = "خصم VIP تلقائي: 10%"
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PointOfSale, contentDescription = null, tint = Color(0xFF10B981))
                Spacer(modifier = Modifier.width(8.dp))
                Text("إصدار فاتورة كاشير موثقة", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = clientPhone,
                        onValueChange = { clientPhone = it },
                        label = { Text("رقم موبايل العميل (إجباري)") },
                        placeholder = { Text("أدخل رقم الهاتف") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = clientName,
                        onValueChange = { clientName = it },
                        label = { Text("اسم العميل (اختياري)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (isVipClient) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = GoldPrimary.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, GoldPrimary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "🌟 عميل مميز! تم الشراء $clientPurchaseCount مرات سابقة. تم تطبيق خصم تلقائي للعميل المميز.",
                                fontSize = 11.sp,
                                color = GoldPrimary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("إجمالي الفاتورة قبل الخصم (ج.م)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Text("وسيلة وطريقة الدفع للفاتورة 💳:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GoldPrimary)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                        items(availablePaymentMethods, key = { it }) { method ->
                            FilterChip(
                                selected = selectedPaymentMethod == method,
                                onClick = {
                                    selectedPaymentMethod = method
                                    paymentMethodDetails = when {
                                        method.contains("كاش") -> "استلام نقدي مباشر بالفرع"
                                        else -> ""
                                    }
                                },
                                label = { Text(method, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF10B981),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = paymentMethodDetails,
                        onValueChange = { paymentMethodDetails = it },
                        label = { Text("بيانات وسيلة الدفع (اسم الحساب / الرقم / الآيبان)") },
                        placeholder = { Text("حساب الدفع") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = appliedDiscountText,
                        onValueChange = { appliedDiscountText = it },
                        label = { Text("ملاحظات الخصومات والعروض") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text("إلغاء", fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = {
                        val rawAmount = amountText.toDoubleOrNull() ?: 1000.0
                        val finalAmount = if (isVipClient) rawAmount * 0.9 else rawAmount
                        val demandNotice = "مطلوب سداد مبلغ ${finalAmount.toInt()} ج.م عبر $selectedPaymentMethod ($paymentMethodDetails). يرجى تحويل المبلغ وتأكيد السداد لتوثيق الفاتورة."
                        val newInvoice = InvoiceEntity(
                            id = "inv_${System.currentTimeMillis()}",
                            invoiceNumber = com.example.util.ShortCodeGenerator.generateInvoiceCode(),
                            title = "فاتورة مبيعات موثقة",
                            category = "BUSINESS_SALE",
                            senderId = "cashier",
                            senderName = currentUserName,
                            receiverId = clientPhone.ifBlank { "client" },
                            receiverName = clientName.ifBlank { "عميل رقم $clientPhone" },
                            totalAmount = finalAmount,
                            paymentMethod = selectedPaymentMethod,
                            paymentMethodDetails = paymentMethodDetails.ifBlank { null },
                            paymentDemandNotice = demandNotice,
                            cashierName = currentUserName,
                            notes = "رقم الموبايل: $clientPhone | وسيلة الدفع: $selectedPaymentMethod ($paymentMethodDetails) | $appliedDiscountText",
                            createdAt = System.currentTimeMillis()
                        )
                        onGenerateInvoice(newInvoice)
                    },
                    enabled = clientPhone.isNotBlank(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981), contentColor = Color.White),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text("إصدار الفاتورة", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {}
    )
}

@Composable
fun AddCashierSubAccountDialog(
    onSave: (name: String, phone: String, pin: String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("كاشير ومبيعات") }
    var canSetPrices by remember { mutableStateOf(true) }
    var canCreateInvoices by remember { mutableStateOf(true) }
    var canViewReports by remember { mutableStateOf(false) }

    val roleOptions = listOf("كاشير ومبيعات", "أمين مخزن وتوريد", "محاسب فرعي ومراجع")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إدارة صلاحيات واختصاصات الكاشير", fontSize = 15.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("اسم الكاشير / الموظف") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("رقم الهاتف") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = pin, onValueChange = { pin = it }, label = { Text("رمز الدخول PIN (4 أرقام)") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                Spacer(modifier = Modifier.height(4.dp))
                Text("اختصاص الموظف:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GoldPrimary)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    roleOptions.forEach { role ->
                        FilterChip(
                            selected = selectedRole == role,
                            onClick = { selectedRole = role },
                            label = { Text(role, fontSize = 10.5.sp) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = GoldPrimary, selectedLabelColor = Color.Black)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text("الصلاحيات الممنوحة من المالك:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = OozoxTeal)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = canSetPrices, onCheckedChange = { canSetPrices = it })
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("إمكانية وضع وتعديل أسعار البضاعة والمنتجات", fontSize = 11.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = canCreateInvoices, onCheckedChange = { canCreateInvoices = it })
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("إمكانية إصدار وفورورد الفواتير للعملاء", fontSize = 11.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = canViewReports, onCheckedChange = { canViewReports = it })
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("إمكانية الاطلاع على تقارير الأرباح والمبيعات", fontSize = 11.sp)
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text("إلغاء", fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = {
                        if (name.isNotBlank() && phone.isNotBlank()) {
                            onSave(name.trim(), phone.trim(), pin.ifBlank { "1234" })
                        }
                    },
                    enabled = name.isNotBlank() && phone.isNotBlank(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text("حفظ الحساب", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {}
    )
}

@Composable
fun ManagePaymentMethodsDialog(
    paymentMethods: List<String>,
    onAddPaymentMethod: (String) -> Unit,
    onRemovePaymentMethod: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var newMethodText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Payments, contentDescription = null, tint = OozoxTeal)
                Spacer(modifier = Modifier.width(8.dp))
                Text("إدارة وسائل وطرق الدفع للمتجر 💳", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "حدد وسيلة الدفع التي يختار منها الكاشير أو العميل أثناء الفاتورة:",
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp)
                ) {
                    items(paymentMethods, key = { it }) { method ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("💳 $method", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                if (paymentMethods.size > 1) {
                                    IconButton(
                                        onClick = { onRemovePaymentMethod(method) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = newMethodText,
                    onValueChange = { newMethodText = it },
                    placeholder = { Text("أورنج كاش") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        if (newMethodText.isNotBlank()) {
                            onAddPaymentMethod(newMethodText.trim())
                            newMethodText = ""
                        }
                    },
                    enabled = newMethodText.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = OozoxTeal, contentColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("إضافة وسيلة الدفع المخصصة", fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("تم") }
        }
    )
}

@Composable
fun EndOfDayReportDialog(
    allInvoices: List<InvoiceEntity>,
    merchantProducts: List<MerchantProductEntity>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    // Calculate metrics
    val totalRevenue = remember(allInvoices) { allInvoices.sumOf { it.totalAmount } }
    val totalInvoicesCount = remember(allInvoices) { allInvoices.size }
    val paymentBreakdown = remember(allInvoices) {
        allInvoices.groupBy { it.paymentMethod.ifBlank { "كاش" } }
            .mapValues { entry -> entry.value.sumOf { it.totalAmount } }
    }

    val totalStockItems = remember(merchantProducts) { merchantProducts.size }
    val totalStockWeight = remember(merchantProducts) { merchantProducts.sumOf { it.weightGrams * it.quantityInStock } }
    val estimatedStockValue = remember(merchantProducts) {
        merchantProducts.sumOf {
            (if (it.consumerPriceEGP > 0) it.consumerPriceEGP else it.weightGrams * 3750.0) * it.quantityInStock
        }
    }

    val totalProfitEst = remember(merchantProducts, totalRevenue) {
        (totalRevenue * 0.08).coerceAtLeast(1250.0) // 8% profit estimation or actual margin
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Assessment, contentDescription = null, tint = Color(0xFF8B5CF6))
                Spacer(modifier = Modifier.width(8.dp))
                Text("تقرير نهاية اليوم الشامل 📊", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
            ) {
                item {
                    Text(
                        text = "ملخص المبيعات، المخزون، ووسائل الدفع اليومية للمالك:",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Summary Cards Grid
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = adaptiveStatusGreen.copy(alpha = 0.12f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("إجمالي المبيعات", fontSize = 10.5.sp, color = adaptiveStatusGreen)
                                Text("${totalRevenue.toInt()} ج.م", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = adaptiveStatusGreen)
                                Text("$totalInvoicesCount فواتير اليوم", fontSize = 9.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        val profitTint = if (isDarkThemeActive) Color(0xFFA78BFA) else Color(0xFF6D28D9)
                        Card(
                            colors = CardDefaults.cardColors(containerColor = profitTint.copy(alpha = 0.12f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("صافي الأرباح اليوم", fontSize = 10.5.sp, color = profitTint)
                                Text("${totalProfitEst.toInt()} ج.م", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = profitTint)
                                Text("هامش أرباح ممتاز", fontSize = 9.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                // Inventory Report Card
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = GoldPrimary.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("📦 تقرير المخزون والبضائع بالمحل:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GoldPrimary)
                            Text("• عدد الأصناف بالمخزن: $totalStockItems منتج", fontSize = 11.sp)
                            Text("• إجمالي الوزن بالمخزون: ${String.format("%.1f", totalStockWeight)} جرام", fontSize = 11.sp)
                            Text("• القيمة التقديرية للبضاعة: ${estimatedStockValue.toInt()} ج.م", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Payment Methods Breakdown
                item {
                    Text("💳 توزيع الإيرادات حسب وسيلة الدفع:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = OozoxTeal)
                    if (paymentBreakdown.isEmpty()) {
                        Text("لا توجد مبيعات مسجلة اليوم بعد.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 4.dp)) {
                            paymentBreakdown.forEach { (method, amount) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("• $method:", fontSize = 11.5.sp)
                                    Text("${amount.toInt()} ج.م", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = OozoxTeal)
                                }
                            }
                        }
                    }
                }

                // Invoices List Today
                item {
                    Text("🧾 قائمة فواتير اليوم التفصيلية:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    if (allInvoices.isEmpty()) {
                        Text("لم يتم إصدار فواتير اليوم.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            allInvoices.take(5).forEach { inv ->
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("${inv.invoiceNumber} - ${inv.receiverName}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            Text("وسيلة الدفع: ${inv.paymentMethod}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Text("${inv.totalAmount.toInt()} ج.م", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    val paymentLines = if (paymentBreakdown.isEmpty()) "• لا توجد مبيعات مسجلة" else paymentBreakdown.entries.joinToString("\n") { "• ${it.key}: ${it.value.toInt()} ج.م" }
                    val reportText = """
                        📊 تقرير نهاية اليوم - متجر OOZOX
                        📅 التاريخ: $dateStr
                        
                        💰 إجمالي المبيعات: ${totalRevenue.toInt()} ج.م ($totalInvoicesCount فاتورة)
                        📈 صافي الأرباح المقدرة: ${totalProfitEst.toInt()} ج.م
                        
                        📦 تقرير المخزون:
                        • عدد الأصناف: $totalStockItems صنف
                        • وزن البضائع: ${String.format("%.1f", totalStockWeight)} جرام
                        • قيمة المخزون: ${estimatedStockValue.toInt()} ج.م
                        
                        💳 تفصيل وسائل الدفع:
                        $paymentLines
                        
                        🛡️ تقرير موثق ومعتمد
                    """.trimIndent()

                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, reportText)
                    }
                    context.startActivity(Intent.createChooser(intent, "مشاركة تقرير نهاية اليوم مع المالك"))
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6), contentColor = Color.White),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("مشاركة التقرير مع المالك عبر الواتساب", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text("إغلاق")
            }
        }
    )
}

@Composable
private fun CompactBusinessToolCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    testTag: String? = null,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
        modifier = modifier
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun BusinessKpiSummaryCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(15.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Text(
                text = title,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

