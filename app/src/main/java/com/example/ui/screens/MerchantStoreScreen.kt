package com.example.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import com.example.data.model.*
import com.example.ui.components.CoverPhotoEditorDialog
import com.example.ui.components.ImageFrameAspectMode
import com.example.ui.components.MediaChooserKind
import com.example.ui.components.MediaPreviewItem
import com.example.ui.components.OozoxCircleIconButton
import com.example.ui.components.MediaPreviewModal
import com.example.ui.components.MediaSourceChooserCard
import com.example.ui.components.OozoxAvatarImage
import com.example.ui.components.OozoxThumbnailImage
import com.example.ui.components.PanoramicProductScanDialog
import com.example.ui.components.VideoEditorDialog
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import com.example.util.BusinessFieldTemplate
import com.example.util.ClientCardPrefsStore
import com.example.util.ClientCardRenderer
import com.example.util.ClientCardStyleConfig
import com.example.util.FileUtils
import com.example.util.GoldUtils
import com.example.util.MerchantFieldCatalog
import com.example.util.ProductMediaItem
import com.example.util.SocialSharePlatform

private enum class StoreImagePickTarget {
    AVATAR,
    COVER
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MerchantStoreScreen(
    viewModel: MainViewModel,
    merchantId: String? = null,
    isManageModeRequested: Boolean = false,
    onBack: () -> Unit,
    onOpenChat: (String) -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val goldPrices by viewModel.goldPrices.collectAsStateWithLifecycle()
    val allMerchantProducts by viewModel.allMerchantProducts.collectAsStateWithLifecycle()
    val allNearbyUsers by viewModel.allNearbyUsers.collectAsStateWithLifecycle()
    val allProjects by viewModel.allProjects.collectAsStateWithLifecycle()
    val allInvoices by viewModel.allInvoices.collectAsStateWithLifecycle()

    val targetMerchantId = merchantId ?: currentUser?.id ?: "nearby_1"
    val isOwnStore = (currentUser?.id == targetMerchantId) || (merchantId == null && isManageModeRequested)

    // Selected or custom business field (not restricted to gold stores)
    var selectedBusinessField by remember(targetMerchantId) {
        mutableStateOf(MerchantFieldCatalog.getSelectedField(context, targetMerchantId))
    }
    val fieldTemplate = remember(selectedBusinessField) {
        MerchantFieldCatalog.templateForName(context, selectedBusinessField)
    }

    // Find target merchant profile data (from nearby or current user or fallback)
    val nearbyMerchant = allNearbyUsers.find { it.id == targetMerchantId }
    val baseUserForCard = remember(currentUser, nearbyMerchant, targetMerchantId, isOwnStore) {
        if (isOwnStore && currentUser != null) {
            currentUser!!
        } else {
            UserEntity(
                id = targetMerchantId,
                name = nearbyMerchant?.name ?: currentUser?.name ?: "صاحب المتجر",
                phone = nearbyMerchant?.phone ?: currentUser?.phone ?: "",
                username = nearbyMerchant?.username ?: currentUser?.username ?: targetMerchantId.take(8),
                traderShopName = nearbyMerchant?.shopName,
                avatarUri = nearbyMerchant?.avatarUri,
                jobTitle = nearbyMerchant?.jobTitle,
                skills = nearbyMerchant?.skills,
                merchantBio = nearbyMerchant?.bio,
                ratingScore = nearbyMerchant?.ratingScore ?: 4.9f,
                totalReviewsCount = nearbyMerchant?.totalReviewsCount ?: 24
            )
        }
    }
    var cardConfig by remember(targetMerchantId, baseUserForCard) {
        mutableStateOf(ClientCardPrefsStore.load(context, baseUserForCard))
    }

    val avatarImageUri = if (isOwnStore) currentUser?.avatarUri else nearbyMerchant?.avatarUri
    val coverImageUri = if (isOwnStore) {
        currentUser?.coverUri?.takeIf { it.isNotBlank() } ?: cardConfig.backgroundImageUri
    } else {
        cardConfig.backgroundImageUri
    }

    var pendingAvatarEditUri by remember { mutableStateOf<Uri?>(null) }
    var pendingCoverEditUri by remember { mutableStateOf<Uri?>(null) }
    var activeStoreMediaPreview by remember { mutableStateOf<MediaPreviewItem?>(null) }
    var activeStoreImageTarget by remember { mutableStateOf<StoreImagePickTarget?>(null) }
    var pendingStoreLaunchTarget by remember { mutableStateOf<StoreImagePickTarget?>(null) }
    var tempStoreCameraUri by remember { mutableStateOf<Uri?>(null) }

    val storeGalleryPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        val target = pendingStoreLaunchTarget
        pendingStoreLaunchTarget = null
        if (uri != null) {
            if (target == StoreImagePickTarget.COVER) {
                pendingCoverEditUri = uri
            } else {
                pendingAvatarEditUri = uri
            }
        }
    }

    val storeCameraPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val rawUri = tempStoreCameraUri
        val target = pendingStoreLaunchTarget
        pendingStoreLaunchTarget = null
        if (success && rawUri != null) {
            if (target == StoreImagePickTarget.COVER) {
                pendingCoverEditUri = rawUri
            } else {
                pendingAvatarEditUri = rawUri
            }
        }
    }

    val launchStoreCameraPhoto: (StoreImagePickTarget) -> Unit = { target ->
        try {
            val dir = File(context.cacheDir, "store_photos").apply { mkdirs() }
            val photoFile = File(dir, "STORE_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", photoFile)
            tempStoreCameraUri = uri
            pendingStoreLaunchTarget = target
            storeCameraPhotoLauncher.launch(uri)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    val storeCameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        val target = pendingStoreLaunchTarget
        if (granted && target != null) {
            launchStoreCameraPhoto(target)
        } else if (!granted) {
            pendingStoreLaunchTarget = null
            Toast.makeText(context, "يرجى السماح بصلاحية الكاميرا", Toast.LENGTH_SHORT).show()
        }
    }

    fun triggerStoreCameraWithPermission(target: StoreImagePickTarget) {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            launchStoreCameraPhoto(target)
        } else {
            pendingStoreLaunchTarget = target
            storeCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val merchantDisplayName = if (isOwnStore) {
        currentUser?.traderShopName?.takeIf { it.isNotBlank() }
            ?: currentUser?.name?.takeIf { it.isNotBlank() }?.let { "متجر $it" }
            ?: "متجر ${fieldTemplate.nameAr}"
    } else {
        nearbyMerchant?.shopName?.takeIf { it.isNotBlank() }
            ?: nearbyMerchant?.name
            ?: "متجر ${fieldTemplate.nameAr}"
    }

    val merchantOwnerName = if (isOwnStore) {
        currentUser?.name?.takeIf { it.isNotBlank() } ?: "مدير المتجر"
    } else {
        nearbyMerchant?.name ?: "صاحب المتجر"
    }

    val merchantPhone = if (isOwnStore) {
        currentUser?.phone ?: ""
    } else {
        nearbyMerchant?.phone ?: ""
    }

    val merchantJobTitle = if (isOwnStore) {
        currentUser?.jobTitle?.takeIf { it.isNotBlank() } ?: fieldTemplate.suggestedJobTitle
    } else {
        nearbyMerchant?.jobTitle?.takeIf { it.isNotBlank() } ?: fieldTemplate.suggestedJobTitle
    }

    val merchantSkills = if (isOwnStore) {
        currentUser?.skills?.takeIf { it.isNotBlank() } ?: fieldTemplate.suggestedSkills.joinToString(", ")
    } else {
        nearbyMerchant?.skills?.takeIf { it.isNotBlank() } ?: fieldTemplate.suggestedSkills.joinToString(", ")
    }

    val merchantBio = if (isOwnStore) {
        currentUser?.merchantBio?.takeIf { it.isNotBlank() } ?: fieldTemplate.suggestedBio
    } else {
        nearbyMerchant?.bio?.takeIf { it.isNotBlank() } ?: fieldTemplate.suggestedBio
    }

    val merchantCity = if (isOwnStore) {
        currentUser?.city?.takeIf { it.isNotBlank() } ?: "القاهرة"
    } else {
        "معتمد على OOZOX"
    }
    val merchantExpYears = if (isOwnStore) (currentUser?.experienceYears ?: 8) else 10
    val showSkillsPublicly = if (isOwnStore) (currentUser?.showSkillsPublicly ?: true) else true

    // Fetch store specific data
    val unclosedSalesFlow = remember(targetMerchantId) { viewModel.getUnclosedSalesForMerchant(targetMerchantId) }
    val unclosedSales by unclosedSalesFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    val closingReportsFlow = remember(targetMerchantId) { viewModel.getClosingReportsForMerchant(targetMerchantId) }
    val closingReports by closingReportsFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    // Fetch merchant customer reviews
    val reviewsFlow = remember(targetMerchantId) { viewModel.getReviewsForMerchant(targetMerchantId) }
    val customerReviews by reviewsFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    val storeProducts = allMerchantProducts.filter { it.merchantId == targetMerchantId || (targetMerchantId == "nearby_1" && it.merchantId == "c1Id") || targetMerchantId.isEmpty() }
    val totalProductViews = remember(storeProducts) { storeProducts.sumOf { it.viewsCount } }
    var viewedProductIdsThisSession by remember { mutableStateOf<Set<String>>(emptySet()) }
    fun recordProductView(productId: String) {
        if (productId !in viewedProductIdsThisSession) {
            viewedProductIdsThisSession = viewedProductIdsThisSession + productId
            viewModel.incrementMerchantProductViews(productId)
        }
    }

    var selectedTab by remember { mutableIntStateOf(if (isOwnStore && isManageModeRequested) 1 else 0) }
    var selectedCategoryFilter by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    // Dialog States
    var showAddProductDialog by remember { mutableStateOf(false) }
    var showPanoramicScanDialog by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showAddReviewDialog by remember { mutableStateOf(false) }
    var showUpdateStockDialog by remember { mutableStateOf(false) }
    var showCloseReportDialog by remember { mutableStateOf(false) }
    var showRecordSaleDialog by remember { mutableStateOf(false) }
    var selectedProductForDetail by remember { mutableStateOf<MerchantProductEntity?>(null) }

    val currentStockWeight = currentUser?.traderTotalGoldWeight ?: 350.0

    // Rating calculations
    val averageRating = if (customerReviews.isNotEmpty()) {
        customerReviews.map { it.rating }.average().toFloat()
    } else {
        nearbyMerchant?.ratingScore ?: 4.95f
    }
    val totalReviewsCount = if (customerReviews.isNotEmpty()) customerReviews.size else (nearbyMerchant?.totalReviewsCount ?: 48)
    val completedDealsCount = allInvoices.count { it.status == InvoiceStatus.PAID.name } + storeProducts.size

    fun shareStoreRichPost(platform: SocialSharePlatform = SocialSharePlatform.ALL) {
        val shareUser = baseUserForCard.copy(
            name = merchantOwnerName,
            avatarUri = avatarImageUri,
            coverUri = coverImageUri,
            traderShopName = merchantDisplayName,
            jobTitle = merchantJobTitle,
            skills = merchantSkills,
            merchantBio = merchantBio,
            ratingScore = averageRating,
            totalReviewsCount = totalReviewsCount
        )
        val shareConfig = cardConfig.copy(
            displayName = merchantDisplayName,
            roleTitle = "${fieldTemplate.nameAr} • $merchantJobTitle",
            bioText = merchantBio,
            storeText = "$merchantDisplayName ($merchantCity)",
            phoneText = merchantPhone,
            backgroundImageUri = coverImageUri,
            includeStore = true,
            includeBio = true,
            includeReliability = true,
            includePhone = merchantPhone.isNotBlank()
        )
        ClientCardRenderer.shareCardToSocialPlatform(
            context = context,
            user = shareUser,
            config = shareConfig,
            trustScore = ((averageRating / 5f) * 100f).toInt().coerceIn(85, 100),
            reliabilitySummary = "متجر ${fieldTemplate.nameAr} موثق على OOZOX • التقييم ${String.format("%.1f", averageRating)}",
            platform = platform,
            projectsCount = allProjects.size,
            completedDealsCount = completedDealsCount
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isOwnStore) "إدارة المتجر" else "ملف المتجر",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = GoldDark.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, GoldPrimary)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.Verified, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(fieldTemplate.nameAr, fontSize = 10.sp, color = adaptiveGoldPrimary, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                        Text(
                            text = merchantDisplayName,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = adaptiveGoldPrimary)
                    }
                },
                actions = {
                    IconButton(
                        onClick = { shareStoreRichPost(SocialSharePlatform.ALL) },
                        modifier = Modifier.testTag("share_store_top_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "مشاركة رابط المتجر", tint = GoldPrimary)
                    }
                    if (isOwnStore) {
                        IconButton(
                            onClick = { showEditProfileDialog = true },
                            modifier = Modifier.testTag("edit_store_top_button")
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "تعديل المتجر والمجال", tint = GoldPrimary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Mode Tabs (If Own Store)
            if (isOwnStore) {
                SecondaryTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = GoldPrimary,
                    indicator = {
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(selectedTab),
                            color = GoldPrimary,
                            height = 3.dp
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedTab = 0
                        },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Storefront, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "الملف العام والمعرض",
                                    fontSize = 12.sp,
                                    fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTab == 0) GoldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedTab = 1
                        },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.PointOfSale, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "إدارة الجرد واليوميات",
                                    fontSize = 12.sp,
                                    fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTab == 1) GoldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    )
                }
            }

            if (selectedTab == 0 || !isOwnStore) {
                // PUBLIC MERCHANT STOREFRONT & PROFILE
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Hero Merchant Profile Header Card
                    item {
                        MerchantProfileHeaderCard(
                            displayName = merchantDisplayName,
                            ownerName = merchantOwnerName,
                            phone = merchantPhone,
                            jobTitle = merchantJobTitle,
                            fieldName = fieldTemplate.nameAr,
                            isGoldField = fieldTemplate.id == "gold_jewelry",
                            avatarUri = avatarImageUri,
                            coverImageUri = coverImageUri,
                            city = merchantCity,
                            experienceYears = merchantExpYears,
                            ratingScore = averageRating,
                            totalReviews = totalReviewsCount,
                            totalProductViews = totalProductViews,
                            isOwnStore = isOwnStore,
                            onChatClick = { onOpenChat(targetMerchantId) },
                            onCallClick = {
                                val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$merchantPhone"))
                                context.startActivity(dialIntent)
                            },
                            onEditProfileClick = { showEditProfileDialog = true },
                            onShareStoreClick = { shareStoreRichPost(SocialSharePlatform.ALL) },
                            onCopyStoreLinkClick = {
                                val storeLink = "https://oozox.app/store/${baseUserForCard.username.ifBlank { targetMerchantId }}"
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("رابط المتجر", storeLink))
                                Toast.makeText(context, "تم نسخ رابط المتجر", Toast.LENGTH_SHORT).show()
                            },
                            onAvatarPreviewClick = if (!avatarImageUri.isNullOrBlank()) {
                                {
                                    activeStoreMediaPreview = MediaPreviewItem(
                                        uri = avatarImageUri,
                                        caption = merchantDisplayName,
                                        senderName = merchantOwnerName,
                                        isVideo = false
                                    )
                                }
                            } else null,
                            onCoverPreviewClick = if (!coverImageUri.isNullOrBlank()) {
                                {
                                    activeStoreMediaPreview = MediaPreviewItem(
                                        uri = coverImageUri,
                                        caption = "غلاف $merchantDisplayName",
                                        senderName = merchantOwnerName,
                                        isVideo = false
                                    )
                                }
                            } else null,
                            onChangeAvatarClick = if (isOwnStore) {
                                { activeStoreImageTarget = StoreImagePickTarget.AVATAR }
                            } else null,
                            onChangeCoverClick = if (isOwnStore) {
                                { activeStoreImageTarget = StoreImagePickTarget.COVER }
                            } else null
                        )
                    }

                    // 2. Skills, Roles & Services Section (الوظائف والمهارات والخدمات)
                    item {
                        MerchantSkillsAndServicesCard(
                            jobTitle = merchantJobTitle,
                            bio = merchantBio,
                            skillsString = merchantSkills,
                            showSkillsPublicly = showSkillsPublicly,
                            isOwnStore = isOwnStore,
                            onEditClick = { showEditProfileDialog = true }
                        )
                    }

                    // 3. Products Showcase & Catalog Header + Category Filters
                    item {
                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (fieldTemplate.id == "gold_jewelry") Icons.Default.Diamond else Icons.Default.Storefront,
                                        contentDescription = null,
                                        tint = GoldPrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (fieldTemplate.id == "gold_jewelry") {
                                            "معرض المشغولات والسبائك (${storeProducts.size})"
                                        } else {
                                            "معرض المنتجات والخدمات (${storeProducts.size})"
                                        },
                                        fontSize = 15.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                }

                                if (isOwnStore) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        OozoxCircleIconButton(
                                            onClick = { showPanoramicScanDialog = true },
                                            imageVector = Icons.Default.Rotate90DegreesCcw,
                                            contentDescription = "360°",
                                            tint = OozoxTeal,
                                            containerColor = OozoxTeal.copy(alpha = 0.16f),
                                            modifier = Modifier.testTag("open_360_scan_card_btn")
                                        )
                                        FilledTonalButton(
                                            onClick = { showAddProductDialog = true },
                                            colors = ButtonDefaults.filledTonalButtonColors(
                                                containerColor = GoldPrimary.copy(alpha = 0.16f),
                                                contentColor = adaptiveGoldPrimary
                                            ),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.height(40.dp)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("إضافة منتج", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Search bar in catalog
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("بحث في المعرض...", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = adaptiveGoldPrimary) },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(Icons.Default.Close, contentDescription = "مسح", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = adaptiveGoldPrimary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                )
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Categories Filter Row
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                item {
                                    val isAllSelected = selectedCategoryFilter == null
                                    FilterChip(
                                        selected = isAllSelected,
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            selectedCategoryFilter = null
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
                                                "الكل (${storeProducts.size})",
                                                fontSize = 12.sp,
                                                fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = adaptiveGoldPrimary,
                                            selectedLabelColor = adaptiveOnPrimary,
                                            selectedLeadingIconColor = adaptiveOnPrimary
                                        )
                                    )
                                }
                                items(MerchantProductCategory.entries.toTypedArray(), key = { it.name }) { cat ->
                                    val count = storeProducts.count { it.category == cat.name }
                                    val isSelected = selectedCategoryFilter == cat.name
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            selectedCategoryFilter = if (selectedCategoryFilter == cat.name) null else cat.name
                                        },
                                        leadingIcon = if (isSelected) {
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
                                                "${cat.label} ($count)",
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = adaptiveGoldPrimary,
                                            selectedLabelColor = adaptiveOnPrimary,
                                            selectedLeadingIconColor = adaptiveOnPrimary
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // 4. Products List
                    val filteredProducts = storeProducts
                        .filter { selectedCategoryFilter == null || it.category == selectedCategoryFilter }
                        .filter { searchQuery.isBlank() || it.title.contains(searchQuery, ignoreCase = true) || it.description?.contains(searchQuery, ignoreCase = true) == true }

                    if (filteredProducts.isEmpty()) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(18.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.Default.ShoppingBag, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(40.dp))
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "لا توجد قطع معروضة في هذا القسم حالياً.",
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (isOwnStore) "أضف أول منتج أو سبيكة لمعرضك بالنقر على إضافة قطعة" else "تابع التاجر لمعرفة أحدث العروض فور توفرها.",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center
                                    )
                                    if (isOwnStore) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Button(
                                            onClick = { showAddProductDialog = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = adaptiveGoldPrimary, contentColor = adaptiveOnPrimary),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text("إضافة منتج جديد", color = adaptiveOnPrimary, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        items(filteredProducts, key = { it.id }) { product ->
                            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                EnhancedMerchantProductCard(
                                    product = product,
                                    goldPrices = goldPrices,
                                    isGoldField = fieldTemplate.id == "gold_jewelry",
                                    fieldName = fieldTemplate.nameAr,
                                    isOwnStore = isOwnStore,
                                    onDetailClick = {
                                        recordProductView(product.id)
                                        selectedProductForDetail = product
                                    },
                                    onImageClick = { mediaItem ->
                                        recordProductView(product.id)
                                        activeStoreMediaPreview = MediaPreviewItem(
                                            uri = mediaItem.uri,
                                            caption = product.title,
                                            senderName = merchantDisplayName,
                                            isVideo = mediaItem.isVideo
                                        )
                                    },
                                    onDeleteClick = { viewModel.deleteMerchantProduct(product.id) },
                                    onOrderClick = {
                                        recordProductView(product.id)
                                        onOpenChat(targetMerchantId)
                                    }
                                )
                            }
                        }
                    }

                    // 5. Ratings & Customer Reviews Section (نظام التقييم والمراجعات وفق مجال المتجر)
                    item {
                        MerchantRatingAndReviewsCard(
                            averageRating = averageRating,
                            totalReviews = totalReviewsCount,
                            reviews = customerReviews,
                            ratingCriteria = fieldTemplate.ratingCriteria,
                            isOwnStore = isOwnStore,
                            onAddReviewClick = { showAddReviewDialog = true }
                        )
                    }
                }
            } else {
                // MANAGEMENT TAB (OWNER ONLY)
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Item 1: Live Inventory & Daily Sales Cards
                    item {
                        MerchantInventoryOverviewCard(
                            totalStockWeight = currentStockWeight,
                            unclosedSales = unclosedSales,
                            onUpdateStockClick = { showUpdateStockDialog = true },
                            onRecordSaleClick = { showRecordSaleDialog = true },
                            onCloseReportClick = { showCloseReportDialog = true }
                        )
                    }

                    // Item 2: Quick Action Buttons Bar
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { showAddProductDialog = true },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = adaptiveGoldPrimary, contentColor = adaptiveOnPrimary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = adaptiveOnPrimary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("إضافة قطعة جديدة", color = adaptiveOnPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }

                            OutlinedButton(
                                onClick = { showRecordSaleDialog = true },
                                modifier = Modifier.weight(1f),
                                border = BorderStroke(1.dp, adaptiveGoldPrimary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = adaptiveGoldPrimary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("تسجيل بيع سريع", color = adaptiveGoldPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }

                    // Item 3: Today's Unclosed Sales Section
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.ShoppingBag, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "مبيعات اليوم النشطة (${unclosedSales.size})",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    if (unclosedSales.isNotEmpty()) {
                                        TextButton(onClick = { showCloseReportDialog = true }) {
                                            Text("إغلاق اليوم", color = GoldPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                if (unclosedSales.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 20.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "لا توجد مبيعات مسجلة اليوم حتى الآن.\nاستخدم 'تسجيل بيع سريع' لإضافة عملية.",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 13.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        unclosedSales.forEach { sale ->
                                            MerchantSaleItemRow(sale = sale)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Item 4: Previous Daily Closing Reports Archive
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Assessment, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "أرشيف تقارير ختام اليوم المعتمدة",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                if (closingReports.isEmpty()) {
                                    Text(
                                        text = "لم يتم إنشاء تقارير ختامية مسبقاً.",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 13.sp,
                                        modifier = Modifier.padding(vertical = 12.dp)
                                    )
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        closingReports.take(5).forEach { report ->
                                            MerchantClosingReportRow(report = report)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- Dialogs ---

    // 1. Add Product Dialog
    if (showAddProductDialog) {
        EnhancedAddMerchantProductDialog(
            isGoldField = fieldTemplate.id == "gold_jewelry",
            fieldName = fieldTemplate.nameAr,
            onDismiss = { showAddProductDialog = false },
            onConfirm = { title, category, karat, weight, mfg, photoUri, desc ->
                viewModel.addMerchantProduct(
                    title = title,
                    category = category,
                    karat = karat,
                    weightGrams = weight,
                    mfgCostPerGram = mfg,
                    photoUri = photoUri,
                    description = desc
                )
                showAddProductDialog = false
            }
        )
    }

    if (showPanoramicScanDialog) {
        PanoramicProductScanDialog(
            merchantShopName = merchantDisplayName,
            currentStaffName = currentUser?.name,
            onSaveProduct = { prod ->
                viewModel.addMerchantProduct(prod)
                showPanoramicScanDialog = false
            },
            onDismiss = { showPanoramicScanDialog = false }
        )
    }

    // 2. Edit Profile, Business Field, Photos & Suggestions Dialog
    if (showEditProfileDialog) {
        EditMerchantProfileDialog(
            merchantId = targetMerchantId,
            selectedFieldName = selectedBusinessField,
            avatarUri = avatarImageUri,
            coverImageUri = coverImageUri,
            currentJobTitle = merchantJobTitle,
            currentSkills = merchantSkills,
            currentServices = currentUser?.servicesOffered ?: "",
            showSkillsPublicly = showSkillsPublicly,
            currentExpYears = merchantExpYears,
            currentBio = merchantBio,
            currentShopName = currentUser?.traderShopName ?: merchantDisplayName,
            currentAddress = currentUser?.traderShopAddress ?: merchantCity,
            onPickAvatar = {
                activeStoreImageTarget = StoreImagePickTarget.AVATAR
            },
            onPickCoverBg = {
                activeStoreImageTarget = StoreImagePickTarget.COVER
            },
            onFieldChanged = { newField ->
                selectedBusinessField = newField
                MerchantFieldCatalog.setSelectedField(context, targetMerchantId, newField)
            },
            onDismiss = { showEditProfileDialog = false },
            onSave = { job, skills, services, showPublic, exp, bio, shop, addr ->
                viewModel.updateMerchantProfileInfo(
                    jobTitle = job,
                    skills = skills,
                    services = services,
                    showSkills = showPublic,
                    experienceYears = exp,
                    bio = bio,
                    shopName = shop,
                    address = addr
                )
                showEditProfileDialog = false
            }
        )
    }

    // 3. Add Review Dialog
    if (showAddReviewDialog) {
        AddMerchantReviewDialog(
            merchantName = merchantDisplayName,
            onDismiss = { showAddReviewDialog = false },
            onSubmit = { overall, acc, spd, hns, comment ->
                viewModel.addMerchantReview(
                    merchantId = targetMerchantId,
                    rating = overall,
                    ratingAccuracy = acc,
                    ratingSpeed = spd,
                    ratingHonesty = hns,
                    comment = comment
                ) {
                    showAddReviewDialog = false
                }
            }
        )
    }

    // 4. Product Detail Dialog (with live view counter)
    selectedProductForDetail?.let { product ->
        val liveProduct = storeProducts.find { it.id == product.id } ?: product
        ProductDetailDialog(
            product = liveProduct,
            goldPrices = goldPrices,
            isGoldField = fieldTemplate.id == "gold_jewelry",
            fieldName = fieldTemplate.nameAr,
            onDismiss = { selectedProductForDetail = null },
            onOrderClick = {
                selectedProductForDetail = null
                onOpenChat(targetMerchantId)
            }
        )
    }

    // 5. Update Stock Weight Dialog
    if (showUpdateStockDialog) {
        UpdateMerchantStockDialog(
            currentStock = currentStockWeight,
            onDismiss = { showUpdateStockDialog = false },
            onConfirm = { newWeight ->
                viewModel.updateTraderStockWeight(newWeight)
                showUpdateStockDialog = false
            }
        )
    }

    // 6. Record Quick Sale Dialog
    if (showRecordSaleDialog) {
        RecordMerchantSaleDialog(
            goldPrices = goldPrices,
            onDismiss = { showRecordSaleDialog = false },
            onConfirm = { title, grams, karat, priceGram, mfgGram, totalEgp, profitEgp, cName, cPhone ->
                viewModel.recordMerchantSale(
                    productId = null,
                    productTitle = title,
                    weightGrams = grams,
                    karat = karat,
                    pricePerGram = priceGram,
                    mfgCostPerGram = mfgGram,
                    totalEGP = totalEgp,
                    profitEGP = profitEgp,
                    customerName = cName,
                    customerPhone = cPhone
                )
                showRecordSaleDialog = false
            }
        )
    }

    // 7. Close End-of-Day Report Dialog
    if (showCloseReportDialog) {
        CloseDailyReportConfirmDialog(
            unclosedSales = unclosedSales,
            currentStock = currentStockWeight,
            onDismiss = { showCloseReportDialog = false },
            onConfirm = {
                viewModel.executeDailyClosingReport(unclosedSales, currentStockWeight)
                showCloseReportDialog = false
            }
        )
    }

    // 8. Zoom-to-Frame Store Avatar Editor
    pendingAvatarEditUri?.let { uri ->
        CoverPhotoEditorDialog(
            imageUri = uri,
            frameMode = ImageFrameAspectMode.SQUARE_AVATAR,
            title = "صورة البروفايل",
            onSaveCroppedCover = { savedPath ->
                val fileUri = Uri.fromFile(java.io.File(savedPath)).toString()
                viewModel.updateProfileImages(avatarUri = fileUri, coverUri = null)
                pendingAvatarEditUri = null
                Toast.makeText(context, "تم تحديث صورة المتجر", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { pendingAvatarEditUri = null }
        )
    }

    // 9. Zoom-to-Frame Store Cover Editor
    pendingCoverEditUri?.let { uri ->
        CoverPhotoEditorDialog(
            imageUri = uri,
            frameMode = ImageFrameAspectMode.BANNER,
            title = "غلاف المتجر",
            onSaveCroppedCover = { savedPath ->
                val persisted = ClientCardPrefsStore.persistBackgroundPhoto(
                    context,
                    Uri.fromFile(java.io.File(savedPath)),
                    targetMerchantId
                )
                val updatedCfg = cardConfig.copy(backgroundImageUri = persisted)
                cardConfig = updatedCfg
                ClientCardPrefsStore.save(context, targetMerchantId, updatedCfg)
                viewModel.updateProfileImages(avatarUri = null, coverUri = persisted)
                pendingCoverEditUri = null
                Toast.makeText(context, "تم تحديث غلاف المتجر", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { pendingCoverEditUri = null }
        )
    }

    // 10. Minimal 2-Option Store Image Chooser (Camera / Gallery ONLY)
    activeStoreImageTarget?.let { target ->
        MediaSourceChooserCard(
            kind = MediaChooserKind.PHOTO,
            onCameraClick = {
                activeStoreImageTarget = null
                triggerStoreCameraWithPermission(target)
            },
            onGalleryClick = {
                activeStoreImageTarget = null
                pendingStoreLaunchTarget = target
                storeGalleryPhotoLauncher.launch("image/*")
            },
            onDismiss = { activeStoreImageTarget = null }
        )
    }

    // 11. Fullscreen Photo/Video Modal
    activeStoreMediaPreview?.let { item ->
        MediaPreviewModal(
            item = item,
            onDismiss = { activeStoreMediaPreview = null }
        )
    }
}

// -------------------------------------------------------------
// Component 1: Merchant Profile Header Card
// -------------------------------------------------------------
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MerchantProfileHeaderCard(
    displayName: String,
    ownerName: String,
    phone: String,
    jobTitle: String,
    fieldName: String = "تجارة عامة",
    isGoldField: Boolean = false,
    avatarUri: String? = null,
    coverImageUri: String? = null,
    city: String,
    experienceYears: Int,
    ratingScore: Float,
    totalReviews: Int,
    totalProductViews: Int = 0,
    isOwnStore: Boolean,
    onChatClick: () -> Unit,
    onCallClick: () -> Unit,
    onEditProfileClick: () -> Unit,
    onShareStoreClick: () -> Unit = {},
    onCopyStoreLinkClick: () -> Unit = {},
    onAvatarPreviewClick: (() -> Unit)? = null,
    onCoverPreviewClick: (() -> Unit)? = null,
    onChangeAvatarClick: (() -> Unit)? = null,
    onChangeCoverClick: (() -> Unit)? = null
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(22.dp),
        tonalElevation = 2.dp,
        border = BorderStroke(0.9.dp, GoldPrimary.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(enabled = isOwnStore) { onEditProfileClick() }
    ) {
        Column {
            // Cover / Card Background Banner (Uncropped Full Fit over Ambient Gradient + Tap to preview)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(156.dp)
                    .background(
                        Brush.linearGradient(
                            if (isDark) {
                                listOf(Color(0xFF1F1B15), Color(0xFF15120D), Color(0xFF2B2318))
                            } else {
                                listOf(Color(0xFFFDF8EE), Color(0xFFF5EBDC), Color(0xFFFAF3E6))
                            }
                        )
                    )
                    .clickable(enabled = onCoverPreviewClick != null) { onCoverPreviewClick?.invoke() }
            ) {
                if (!coverImageUri.isNullOrBlank()) {
                    OozoxThumbnailImage(
                        model = coverImageUri,
                        width = 640.dp,
                        height = 156.dp,
                        shape = RoundedCornerShape(0.dp),
                        contentScale = ContentScale.Fit,
                        contentDescription = "خلفية كارت المتجر",
                        onClick = onCoverPreviewClick
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Black.copy(alpha = 0.25f),
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.35f)
                                    )
                                )
                            )
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Surface(
                        color = if (isDark || !coverImageUri.isNullOrBlank()) Color.Black.copy(alpha = 0.62f) else Color.White.copy(alpha = 0.94f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(0.8.dp, GoldPrimary.copy(alpha = 0.55f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                        ) {
                            Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(fieldName, color = GoldPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isOwnStore && onChangeCoverClick != null) {
                            Surface(
                                color = Color.Black.copy(alpha = 0.66f),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(0.8.dp, GoldPrimary.copy(alpha = 0.65f)),
                                modifier = Modifier
                                    .testTag("change_store_cover_button")
                                    .clickable { onChangeCoverClick() }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                                ) {
                                    Icon(Icons.Default.Wallpaper, contentDescription = "تغيير خلفية الكارت", tint = GoldPrimary, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("خلفية الكارت", color = Color.White, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        } else {
                            Surface(
                                color = if (isDark || !coverImageUri.isNullOrBlank()) Color.Black.copy(alpha = 0.62f) else Color.White.copy(alpha = 0.92f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = "خبرة $experienceYears سنوات",
                                    color = if (!coverImageUri.isNullOrBlank()) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Profile Info & Overlapping Avatar Row
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(72.dp)) {
                        if (!avatarUri.isNullOrBlank()) {
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable(enabled = onAvatarPreviewClick != null) { onAvatarPreviewClick?.invoke() }
                            ) {
                                OozoxAvatarImage(
                                    model = avatarUri,
                                    name = displayName,
                                    size = 72.dp,
                                    modifier = Modifier.border(2.dp, GoldPrimary, CircleShape),
                                    contentDescription = "صورة المتجر"
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(68.dp)
                                    .clip(CircleShape)
                                    .background(if (isDark) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant)
                                    .border(2.dp, GoldPrimary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Storefront, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(30.dp))
                            }
                        }

                        if (isOwnStore && onChangeAvatarClick != null) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(GoldPrimary)
                                    .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
                                    .testTag("change_store_avatar_button")
                                    .clickable { onChangeAvatarClick() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = "تغيير صورة البروفايل", tint = Color.Black, modifier = Modifier.size(14.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = displayName,
                                fontSize = 17.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.Default.Verified, contentDescription = "موثق", tint = GoldPrimary, modifier = Modifier.size(17.dp))
                        }

                        Text(
                            text = jobTitle,
                            fontSize = 12.sp,
                            color = adaptiveGoldPrimary,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(3.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = city, fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }

                // Trust & Views Metrics Grid
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (isDark) Color.Black.copy(alpha = 0.28f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                        .padding(vertical = 10.dp, horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(13.dp))
                            Text(
                                text = String.format("%.2f", ratingScore),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = GoldPrimary
                            )
                        }
                        Text("$totalReviews تقييم", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Box(modifier = Modifier.width(1.dp).height(24.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)))

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.Visibility, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(14.dp))
                            Text(
                                text = "$totalProductViews",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFF38BDF8)
                            )
                        }
                        Text("مشاهدات المعرض", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Box(modifier = Modifier.width(1.dp).height(24.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)))

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("100%", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF10B981))
                        Text(if (isGoldField) "دقة العيار" else "الموثوقية", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // Action Buttons (Includes rich social store share + link copy)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isOwnStore) {
                        Button(
                            onClick = onEditProfileClick,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("edit_store_profile_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = adaptiveGoldPrimary, contentColor = adaptiveOnPrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("تعديل المتجر والمجال", color = adaptiveOnPrimary, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                        }
                        OutlinedButton(
                            onClick = onShareStoreClick,
                            border = BorderStroke(1.dp, adaptiveGoldPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("share_own_store_button")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = adaptiveGoldPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("مشاركة", color = adaptiveGoldPrimary, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                        }
                        FilledTonalIconButton(
                            onClick = onCopyStoreLinkClick,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Default.Link, contentDescription = "نسخ رابط المتجر", tint = adaptiveGoldPrimary)
                        }
                    } else {
                        Button(
                            onClick = onChatClick,
                            modifier = Modifier.weight(1f).height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = adaptiveGoldPrimary, contentColor = adaptiveOnPrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("محادثة", color = adaptiveOnPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        OutlinedButton(
                            onClick = onCallClick,
                            border = BorderStroke(1.dp, adaptiveGoldPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Text("اتصال", color = GoldPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        FilledTonalIconButton(
                            onClick = onShareStoreClick,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "مشاركة المتجر", tint = adaptiveGoldPrimary)
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Component 2: Merchant Skills, Roles & Services Card
// -------------------------------------------------------------
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MerchantSkillsAndServicesCard(
    jobTitle: String,
    bio: String,
    skillsString: String?,
    showSkillsPublicly: Boolean,
    isOwnStore: Boolean,
    onEditClick: () -> Unit
) {
    val skillsList = remember(skillsString) {
        skillsString?.split(",", "\n")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
    }

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.WorkOutline, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "الوظائف والمهارات والخدمات",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (isOwnStore) {
                    IconButton(onClick = onEditClick, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = GoldPrimary, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Bio
            Text(
                text = bio,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 19.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (skillsList.isNotEmpty() && showSkillsPublicly) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "المهارات والاعتمادات الميدانية:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldAccent
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        skillsList.forEach { skill ->
                            Surface(
                                color = if (isDark) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = GoldPrimary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = skill,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Surface(
                    color = if (isDark) Color(0xFF241C12) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(GoldPrimary.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Schedule, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(20.dp))
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "قريباً — توثيق المهارات والخدمات",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = GoldPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "يتم فحص وتدقيق شهادات الخبرة وسجلات الاعتماد.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Component 3: Enhanced Merchant Product Card (With Live Views Counter + Multi-Industry Adaptation)
// -------------------------------------------------------------
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EnhancedMerchantProductCard(
    product: MerchantProductEntity,
    goldPrices: GoldPricesModel,
    isGoldField: Boolean = true,
    fieldName: String = "المجوهرات والذهب",
    isOwnStore: Boolean,
    onDetailClick: () -> Unit,
    onImageClick: (ProductMediaItem) -> Unit = {},
    onDeleteClick: () -> Unit,
    onOrderClick: () -> Unit
) {
    val liveGramsPrice = when (product.karat.uppercase()) {
        "KARAT_24", "24K", "24" -> goldPrices.karat24
        "KARAT_18", "18K", "18" -> goldPrices.karat18
        "KARAT_14", "14K", "14" -> goldPrices.karat14
        "KARAT_999", "999" -> goldPrices.silver999
        "KARAT_925", "925" -> goldPrices.silver925
        "KARAT_800", "800" -> goldPrices.silver800
        else -> goldPrices.karat21
    }

    val totalCalculatedPrice = if (isGoldField) {
        (liveGramsPrice + product.manufacturingCostPerGram) * product.weightGrams
    } else {
        if (product.manufacturingCostPerGram > 0.0) {
            product.manufacturingCostPerGram * product.weightGrams.coerceAtLeast(1.0)
        } else {
            (liveGramsPrice * product.weightGrams)
        }
    }

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "product_card_scale"
    )

    val mediaItems = remember(product.photoUri) { FileUtils.parseProductMedia(product.photoUri) }
    val primaryMedia = mediaItems.firstOrNull()

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 1.dp,
        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onDetailClick
            )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Product Thumbnail representation (Uncropped Fit + Tap opens Fullscreen Preview)
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isDark) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
                        .border(0.8.dp, GoldPrimary.copy(alpha = 0.35f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (primaryMedia != null) {
                        OozoxThumbnailImage(
                            model = primaryMedia.uri,
                            width = 88.dp,
                            height = 88.dp,
                            shape = RoundedCornerShape(14.dp),
                            contentScale = ContentScale.Fit,
                            contentDescription = product.title,
                            onClick = { onImageClick(primaryMedia) }
                        )
                        if (primaryMedia.isVideo) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.65f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                        if (mediaItems.size > 1) {
                            Surface(
                                color = Color.Black.copy(alpha = 0.72f),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(4.dp)
                            ) {
                                Text(
                                    text = "+${mediaItems.size - 1}",
                                    color = Color.White,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    } else {
                        val categoryIconVector = when (product.category) {
                            MerchantProductCategory.BULLION.name -> Icons.Default.ViewModule
                            MerchantProductCategory.JEWELRY.name -> if (isGoldField) Icons.Default.Diamond else Icons.Default.Storefront
                            MerchantProductCategory.GOLD_POUND.name -> Icons.Default.MonetizationOn
                            MerchantProductCategory.SILVER.name -> Icons.Default.AutoAwesome
                            MerchantProductCategory.SERVICES.name -> Icons.Default.Build
                            else -> Icons.Default.Inventory2
                        }
                        Icon(
                            imageVector = categoryIconVector,
                            contentDescription = null,
                            tint = GoldPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Primary Spec Badge (Karat for Gold OR Field Name for Other Industries)
                        Surface(
                            color = GoldDark.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(0.8.dp, GoldPrimary.copy(alpha = 0.7f))
                        ) {
                            Text(
                                text = if (isGoldField) product.karat.replace("KARAT_", "عيار ") else fieldName,
                                fontSize = 10.sp,
                                color = GoldPrimary,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Live Product Views Counter Badge
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.testTag("product_views_badge_${product.id}")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Visibility,
                                        contentDescription = "عدد المشاهدات",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Text(
                                        text = "${product.viewsCount}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            // Availability Tag
                            Surface(
                                color = Color(0xFF10B981).copy(alpha = 0.14f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "متاح",
                                    fontSize = 10.sp,
                                    color = Color(0xFF10B981),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = product.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.5.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    if (isGoldField) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("الوزن: ${GoldUtils.formatWeight(product.weightGrams)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("المصنعية: ${GoldUtils.formatCurrency(product.manufacturingCostPerGram)}/ج", fontSize = 11.sp, color = GoldAccent)
                        }
                    } else if (!product.description.isNullOrBlank()) {
                        Text(
                            text = product.description,
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Price and Order Actions Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isDark) Color.Black.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(if (isGoldField) "السعر التقديري المباشر" else "السعر", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = GoldUtils.formatCurrency(totalCalculatedPrice),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = GoldPrimary
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (isOwnStore) {
                        IconButton(onClick = onDeleteClick, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "حذف", tint = Color(0xFFEF4444), modifier = Modifier.size(19.dp))
                        }
                    }

                    Button(
                        onClick = onOrderClick,
                        colors = ButtonDefaults.buttonColors(containerColor = adaptiveGoldPrimary, contentColor = adaptiveOnPrimary),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.height(38.dp)
                    ) {
                        Text("طلب المنتج", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = adaptiveOnPrimary)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Component 4: Merchant Ratings & Reviews Card (نظام التقييم لترتيب البحث)
// -------------------------------------------------------------
@Composable
fun MerchantRatingAndReviewsCard(
    averageRating: Float,
    totalReviews: Int,
    reviews: List<MerchantReviewEntity>,
    ratingCriteria: Triple<String, String, String> = Triple("جودة التنفيذ والمواصفات", "سرعة المعاملة والتسليم", "الأمانة والنزاهة"),
    isOwnStore: Boolean,
    onAddReviewClick: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "تقييمات العملاء",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (!isOwnStore) {
                    Button(
                        onClick = onAddReviewClick,
                        colors = ButtonDefaults.buttonColors(containerColor = adaptiveGoldPrimary, contentColor = adaptiveOnPrimary),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("إضافة تقييم", color = adaptiveOnPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Rating Scorecard Banner
            Surface(
                color = if (isDark) Color.Black.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        Text(
                            text = String.format("%.1f", averageRating),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = adaptiveGoldPrimary
                        )
                        Row {
                            repeat(5) { index ->
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = if (index < averageRating.toInt()) adaptiveGoldPrimary else MaterialTheme.colorScheme.outlineVariant,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                        Text("$totalReviews تقييم", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Box(modifier = Modifier.width(1.dp).height(50.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)))

                    Spacer(modifier = Modifier.width(14.dp))

                    // 3 Field-Matched Criteria Breakdown
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        RatingCriterionRow(title = ratingCriteria.first, score = "5.0")
                        RatingCriterionRow(title = ratingCriteria.second, score = "4.9")
                        RatingCriterionRow(title = ratingCriteria.third, score = "5.0")
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Reviews List
            if (reviews.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "كن أول من يقيّم هذا التاجر بعد إتمام معاملتك.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    reviews.forEach { rev ->
                        Surface(
                            color = if (isDark) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .background(GoldPrimary.copy(alpha = 0.2f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Person, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(rev.reviewerName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                                            if (rev.isVerifiedBuyer) {
                                                Text("عميل موثق", fontSize = 10.sp, color = Color(0xFF10B981))
                                            }
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Icon(Icons.Default.Star, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(13.dp))
                                        Text(String.format("%.1f", rev.rating), color = GoldPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = rev.comment,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 18.sp
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
fun RatingCriterionRow(title: String, score: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            Icon(Icons.Default.Star, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(11.dp))
            Text(score, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = GoldPrimary)
        }
    }
}

// -------------------------------------------------------------
// Component 5: Edit Profile, Skills & Roles Dialog
// -------------------------------------------------------------
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditMerchantProfileDialog(
    merchantId: String = "default",
    selectedFieldName: String = MerchantFieldCatalog.defaultFields.first().nameAr,
    avatarUri: String? = null,
    coverImageUri: String? = null,
    currentJobTitle: String,
    currentSkills: String,
    currentServices: String,
    showSkillsPublicly: Boolean,
    currentExpYears: Int,
    currentBio: String,
    currentShopName: String,
    currentAddress: String,
    onPickAvatar: () -> Unit = {},
    onPickCoverBg: () -> Unit = {},
    onFieldChanged: (String) -> Unit = {},
    onDismiss: () -> Unit,
    onSave: (job: String, skills: String, services: String, showPublic: Boolean, exp: Int, bio: String, shop: String, addr: String) -> Unit
) {
    val context = LocalContext.current
    var allFields by remember { mutableStateOf(MerchantFieldCatalog.loadAllFields(context)) }
    var activeFieldName by remember(selectedFieldName) { mutableStateOf(selectedFieldName) }
    val activeTemplate = remember(activeFieldName, allFields) {
        MerchantFieldCatalog.templateForName(context, activeFieldName)
    }
    var showCustomFieldRow by remember { mutableStateOf(false) }
    var customFieldInput by remember { mutableStateOf("") }

    var jobTitle by remember { mutableStateOf(currentJobTitle) }
    var skillsText by remember { mutableStateOf(currentSkills) }
    var bioText by remember { mutableStateOf(currentBio) }
    var shopName by remember { mutableStateOf(currentShopName) }
    var address by remember { mutableStateOf(currentAddress) }
    var expYears by remember { mutableIntStateOf(currentExpYears) }
    var showPublicly by remember { mutableStateOf(showSkillsPublicly) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .navigationBarsPadding()
                .imePadding()
                .heightIn(max = 680.dp),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.35f))
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "تعديل المتجر والمجال",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "اختر مجالك أو أنشئ مجالاً جديداً واضبط صور المتجر",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                LazyColumn(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Photos Upload Row (Profile Avatar + Card Cover Background)
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("صور المتجر والكارت", fontSize = 11.5.sp, color = GoldAccent, fontWeight = FontWeight.Bold)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onPickAvatar,
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.6f)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("dialog_pick_avatar_button")
                                ) {
                                    Text(
                                        text = if (!avatarUri.isNullOrBlank()) "تغيير البروفايل" else "صورة البروفايل",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1
                                    )
                                }

                                OutlinedButton(
                                    onClick = onPickCoverBg,
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.6f)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("dialog_pick_cover_button")
                                ) {
                                    Text(
                                        text = if (!coverImageUri.isNullOrBlank()) "تغيير الخلفية" else "خلفية الكارت",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    // Multi-Industry Field Selector + "+ مجال جديد" Creator
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("مجال المتجر", fontSize = 11.5.sp, color = GoldAccent, fontWeight = FontWeight.Bold)
                                TextButton(
                                    onClick = { showCustomFieldRow = !showCustomFieldRow },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.testTag("toggle_custom_field_button")
                                ) {
                                    Text(if (showCustomFieldRow) "إغلاق" else "+ مجال جديد", color = adaptiveGoldPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            if (showCustomFieldRow) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = customFieldInput,
                                        onValueChange = { customFieldInput = it },
                                        placeholder = { Text("مثال: صيانة سيارات", fontSize = 12.sp) },
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("custom_field_input")
                                    )
                                    Button(
                                        onClick = {
                                            val name = customFieldInput.trim()
                                            if (name.isNotBlank()) {
                                                val created = MerchantFieldCatalog.addCustomField(context, name)
                                                allFields = MerchantFieldCatalog.loadAllFields(context)
                                                activeFieldName = created.nameAr
                                                MerchantFieldCatalog.setSelectedField(context, merchantId, created.nameAr)
                                                onFieldChanged(created.nameAr)
                                                customFieldInput = ""
                                                showCustomFieldRow = false
                                            }
                                        },
                                        enabled = customFieldInput.isNotBlank(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = adaptiveGoldPrimary, contentColor = adaptiveOnPrimary),
                                        modifier = Modifier
                                            .height(48.dp)
                                            .testTag("add_custom_field_confirm")
                                    ) {
                                        Text("إضافة", fontWeight = FontWeight.Bold, color = adaptiveOnPrimary)
                                    }
                                }
                            }

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(allFields, key = { it.id }) { field ->
                                    val isSel = field.nameAr.equals(activeFieldName, ignoreCase = true)
                                    FilterChip(
                                        selected = isSel,
                                        onClick = {
                                            activeFieldName = field.nameAr
                                            MerchantFieldCatalog.setSelectedField(context, merchantId, field.nameAr)
                                            onFieldChanged(field.nameAr)
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = adaptiveGoldPrimary,
                                            selectedLabelColor = adaptiveOnPrimary
                                        ),
                                        label = {
                                            Text(
                                                text = field.nameAr,
                                                fontSize = 11.5.sp,
                                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                    )
                                }
                            }

                            // Field Suggestions Apply Strip
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(0.8.dp, GoldPrimary.copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "مقترحات مجال: ${activeTemplate.nameAr}",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = GoldPrimary
                                        )
                                        Text(
                                            text = activeTemplate.suggestedJobTitle,
                                            fontSize = 10.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    TextButton(
                                        onClick = {
                                            jobTitle = activeTemplate.suggestedJobTitle
                                            bioText = activeTemplate.suggestedBio
                                            skillsText = activeTemplate.suggestedSkills.joinToString(", ")
                                        },
                                        modifier = Modifier.testTag("apply_field_suggestions_button")
                                    ) {
                                        Text("تطبيق المقترحات", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = adaptiveGoldPrimary)
                                    }
                                }
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = shopName,
                            onValueChange = { shopName = it },
                            label = { Text("اسم المتجر أو النشاط") },
                            placeholder = { Text("متجر النور الحديث", fontSize = 13.sp) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = jobTitle,
                            onValueChange = { jobTitle = it },
                            label = { Text("المسمى أو التخصص") },
                            placeholder = { Text(activeTemplate.suggestedJobTitle.take(26), fontSize = 13.sp) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = address,
                            onValueChange = { address = it },
                            label = { Text("العنوان") },
                            placeholder = { Text("القاهرة - مدينة نصر", fontSize = 13.sp) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = bioText,
                            onValueChange = { bioText = it },
                            label = { Text("النبذة المهنية") },
                            placeholder = { Text("وصف مختصر لنشاطك", fontSize = 13.sp) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = skillsText,
                            onValueChange = { skillsText = it },
                            label = { Text("المهارات والخدمات (بفاصلة)") },
                            placeholder = { Text("خدمة، جودة، توصيل", fontSize = 13.sp) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 2
                        )
                    }

                    item {
                        Text("مهارات مقترحة للمجال:", fontSize = 11.sp, color = GoldAccent, fontWeight = FontWeight.Bold)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                        ) {
                            activeTemplate.suggestedSkills.forEach { preset ->
                                SuggestionChip(
                                    onClick = {
                                        if (!skillsText.contains(preset)) {
                                            skillsText = if (skillsText.isBlank()) preset else "$skillsText, $preset"
                                        }
                                    },
                                    label = { Text(preset, fontSize = 10.sp) },
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("عرض المهارات للعملاء", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Text("إخفاؤها يعرض حالة قريباً", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = showPublicly,
                                onCheckedChange = { showPublicly = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = GoldPrimary, checkedTrackColor = GoldDark)
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("إلغاء", fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = {
                            onSave(jobTitle, skillsText, "", showPublicly, expYears, bioText, shopName, address)
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = adaptiveGoldPrimary, contentColor = adaptiveOnPrimary),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("حفظ", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Component 6: Add Review Dialog
// -------------------------------------------------------------
@Composable
fun AddMerchantReviewDialog(
    merchantName: String,
    onDismiss: () -> Unit,
    onSubmit: (overall: Float, acc: Float, spd: Float, hns: Float, comment: String) -> Unit
) {
    var overallRating by remember { mutableFloatStateOf(5.0f) }
    var comment by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .navigationBarsPadding()
                .imePadding()
                .wrapContentHeight(),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.35f))
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
                    Text("إضافة تقييم", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    OozoxCircleIconButton(
                        onClick = onDismiss,
                        imageVector = Icons.Default.Close,
                        contentDescription = "إغلاق",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text("تقييمك لـ $merchantName يساعد العملاء.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    (1..5).forEach { star ->
                        IconButton(onClick = { overallRating = star.toFloat() }, modifier = Modifier.size(40.dp)) {
                            Icon(
                                imageVector = if (star <= overallRating) Icons.Default.Star else Icons.Default.StarOutline,
                                contentDescription = "$star نجوم",
                                tint = if (star <= overallRating) adaptiveGoldPrimary else MaterialTheme.colorScheme.outlineVariant,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("مراجعتك") },
                    placeholder = { Text("تعامل ممتاز وجودة عالية", fontSize = 13.sp) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("إلغاء", fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = {
                            if (comment.isNotBlank()) {
                                onSubmit(overallRating, overallRating, overallRating, overallRating, comment.trim())
                            }
                        },
                        enabled = comment.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = adaptiveGoldPrimary, contentColor = adaptiveOnPrimary),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("حفظ", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Component 7: Unified Add Product Dialog (2-Option Camera/Gallery + Editors + All Formats)
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EnhancedAddMerchantProductDialog(
    isGoldField: Boolean = true,
    fieldName: String = "المجوهرات والذهب",
    onDismiss: () -> Unit,
    onConfirm: (title: String, category: MerchantProductCategory, karat: GoldKarat, weight: Double, mfg: Double, photoUri: String?, desc: String?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf("") }
    var selectedCategory by remember {
        mutableStateOf(if (isGoldField) MerchantProductCategory.BULLION else MerchantProductCategory.SERVICES)
    }
    var selectedKarat by remember { mutableStateOf(GoldKarat.KARAT_24) }
    var weightText by remember { mutableStateOf(if (isGoldField) "" else "1") }
    var mfgCostText by remember { mutableStateOf("") }
    var descriptionText by remember { mutableStateOf("") }
    var attachedMedia by remember { mutableStateOf<List<ProductMediaItem>>(emptyList()) }
    var previewMediaItem by remember { mutableStateOf<MediaPreviewItem?>(null) }
    var mediaEditUri by remember { mutableStateOf<Uri?>(null) }
    var pendingVideoEditUri by remember { mutableStateOf<Uri?>(null) }
    var activeProductChooserKind by remember { mutableStateOf<MediaChooserKind?>(null) }
    var pendingProductCameraKind by remember { mutableStateOf<MediaChooserKind?>(null) }
    var tempProductPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var tempProductVideoUri by remember { mutableStateOf<Uri?>(null) }
    var isUploadingMedia by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableFloatStateOf(0f) }

    val multiplePhotosLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            if (uris.size == 1) {
                mediaEditUri = uris.first()
            } else {
                scope.launch {
                    isUploadingMedia = true
                    uploadProgress = 0.1f
                    val total = uris.size.coerceAtLeast(1)
                    val added = mutableListOf<ProductMediaItem>()
                    uris.forEachIndexed { idx, uri ->
                        val processed = FileUtils.smartProcessAndStoreMedia(
                            context = context,
                            sourceUri = uri,
                            isVideo = false,
                            targetFolder = "product_media"
                        ) { itemProg ->
                            uploadProgress = ((idx + itemProg) / total.toFloat()).coerceIn(0f, 1f)
                        }
                        if (processed != null) {
                            added.add(ProductMediaItem(uri = processed.uri, isVideo = false, durationSec = 0))
                        }
                    }
                    isUploadingMedia = false
                    if (added.isNotEmpty()) {
                        attachedMedia = (attachedMedia + added).distinctBy { it.uri }.take(8)
                    }
                }
            }
        }
    }

    val productCameraPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val rawUri = tempProductPhotoUri
        if (success && rawUri != null) {
            mediaEditUri = rawUri
        }
    }

    val productGalleryVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            pendingVideoEditUri = uri
        }
    }

    val productCameraVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CaptureVideo()
    ) { success ->
        val rawUri = tempProductVideoUri
        if (success && rawUri != null) {
            pendingVideoEditUri = rawUri
        }
    }

    val launchProductPhotoCamera: () -> Unit = {
        try {
            val dir = File(context.cacheDir, "product_camera").apply { mkdirs() }
            val file = File(dir, "PROD_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            tempProductPhotoUri = uri
            productCameraPhotoLauncher.launch(uri)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    val launchProductVideoCamera: () -> Unit = {
        try {
            val dir = File(context.cacheDir, "product_camera").apply { mkdirs() }
            val file = File(dir, "PROD_VID_${System.currentTimeMillis()}.mp4")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            tempProductVideoUri = uri
            productCameraVideoLauncher.launch(uri)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    val productCameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        val kind = pendingProductCameraKind
        pendingProductCameraKind = null
        if (granted && kind != null) {
            if (kind == MediaChooserKind.PHOTO) launchProductPhotoCamera() else launchProductVideoCamera()
        } else if (!granted) {
            Toast.makeText(context, "يرجى السماح بصلاحية الكاميرا", Toast.LENGTH_SHORT).show()
        }
    }

    fun triggerProductCamera(kind: MediaChooserKind) {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            if (kind == MediaChooserKind.PHOTO) launchProductPhotoCamera() else launchProductVideoCamera()
        } else {
            pendingProductCameraKind = kind
            productCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    mediaEditUri?.let { editUri ->
        CoverPhotoEditorDialog(
            imageUri = editUri,
            frameMode = ImageFrameAspectMode.PRODUCT_PHOTO,
            title = "صورة المنتج",
            onSaveCroppedCover = { savedPath ->
                val uriStr = Uri.fromFile(java.io.File(savedPath)).toString()
                attachedMedia = (attachedMedia + ProductMediaItem(uri = uriStr, isVideo = false, durationSec = 0))
                    .distinctBy { it.uri }
                    .take(8)
                mediaEditUri = null
            },
            onDismiss = { mediaEditUri = null }
        )
    }

    pendingVideoEditUri?.let { editVidUri ->
        VideoEditorDialog(
            videoUri = editVidUri,
            showCaptionField = false,
            targetFolder = "product_media",
            onConfirmSave = { savedUri, durSec, _ ->
                attachedMedia = (attachedMedia + ProductMediaItem(uri = savedUri.toString(), isVideo = true, durationSec = durSec))
                    .distinctBy { it.uri }
                    .take(8)
                pendingVideoEditUri = null
            },
            onDismiss = { pendingVideoEditUri = null }
        )
    }

    activeProductChooserKind?.let { chooserKind ->
        MediaSourceChooserCard(
            kind = chooserKind,
            onCameraClick = {
                activeProductChooserKind = null
                triggerProductCamera(chooserKind)
            },
            onGalleryClick = {
                activeProductChooserKind = null
                if (chooserKind == MediaChooserKind.PHOTO) {
                    multiplePhotosLauncher.launch("image/*")
                } else {
                    productGalleryVideoLauncher.launch("video/*")
                }
            },
            onDismiss = { activeProductChooserKind = null }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .navigationBarsPadding()
                .imePadding()
                .heightIn(max = 680.dp),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 12.dp,
            border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.45f))
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp),
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
                                .background(GoldPrimary.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddBusiness,
                                contentDescription = null,
                                tint = GoldPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "إضافة منتج للمعرض",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = fieldName,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إغلاق",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                // Body
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Media Picker Box (Minimal 2-option Camera/Gallery trigger for Photo & Video)
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f),
                        border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "صور وفيديو المنتج (${attachedMedia.size}/8)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldPrimary
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { activeProductChooserKind = MediaChooserKind.PHOTO },
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.55f)),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .testTag("product_add_photo_btn")
                                ) {
                                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("إضافة صورة", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = GoldPrimary)
                                }

                                OutlinedButton(
                                    onClick = { activeProductChooserKind = MediaChooserKind.VIDEO },
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .testTag("product_add_video_btn")
                                ) {
                                    Icon(Icons.Default.Videocam, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("إضافة فيديو", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }

                            if (isUploadingMedia) {
                                LinearProgressIndicator(
                                    progress = { uploadProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(5.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = adaptiveGoldPrimary,
                                    trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                                )
                            }

                            if (attachedMedia.isNotEmpty()) {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(attachedMedia, key = { it.uri }) { item ->
                                        Box(modifier = Modifier.size(72.dp)) {
                                            OozoxThumbnailImage(
                                                model = item.uri,
                                                width = 72.dp,
                                                height = 72.dp,
                                                shape = RoundedCornerShape(10.dp),
                                                contentScale = ContentScale.Fit,
                                                contentDescription = if (item.isVideo) "فيديو" else "صورة",
                                                onClick = {
                                                    previewMediaItem = MediaPreviewItem(
                                                        uri = item.uri,
                                                        caption = title.ifBlank { "معاينة" },
                                                        isVideo = item.isVideo
                                                    )
                                                }
                                            )

                                            if (item.isVideo) {
                                                Surface(
                                                    color = Color.Black.copy(alpha = 0.72f),
                                                    shape = RoundedCornerShape(6.dp),
                                                    modifier = Modifier
                                                        .align(Alignment.BottomStart)
                                                        .padding(4.dp)
                                                ) {
                                                    Text(
                                                        text = FileUtils.formatDurationSeconds(item.durationSec.coerceAtLeast(1)),
                                                        color = Color.White,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(3.dp)
                                                    .size(20.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.Black.copy(alpha = 0.75f))
                                                    .clickable {
                                                        attachedMedia = attachedMedia.filterNot { it.uri == item.uri }
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "حذف",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text(if (isGoldField) "اسم القطعة أو السبيكة" else "اسم المنتج أو الخدمة") },
                        placeholder = { Text(if (isGoldField) "سبيكة 10 جرام" else "منتج مميز", fontSize = 13.sp) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    if (isGoldField) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("القسم", fontSize = 11.5.sp, color = GoldAccent, fontWeight = FontWeight.Bold)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                MerchantProductCategory.entries.forEach { cat ->
                                    val isSel = selectedCategory == cat
                                    FilterChip(
                                        selected = isSel,
                                        onClick = {
                                            selectedCategory = cat
                                            if (cat == MerchantProductCategory.BULLION) selectedKarat = GoldKarat.KARAT_24
                                            else if (cat == MerchantProductCategory.GOLD_POUND) selectedKarat = GoldKarat.KARAT_21
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = adaptiveGoldPrimary,
                                            selectedLabelColor = adaptiveOnPrimary
                                        ),
                                        label = {
                                            Text(
                                                text = cat.label,
                                                fontSize = 11.5.sp,
                                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                    )
                                }
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("العيار", fontSize = 11.5.sp, color = GoldAccent, fontWeight = FontWeight.Bold)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                GoldKarat.entries.forEach { karat ->
                                    val isSel = selectedKarat == karat
                                    FilterChip(
                                        selected = isSel,
                                        onClick = { selectedKarat = karat },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = adaptiveGoldPrimary,
                                            selectedLabelColor = adaptiveOnPrimary
                                        ),
                                        label = {
                                            Text(
                                                text = karat.displayLabel(),
                                                fontSize = 11.5.sp,
                                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = weightText,
                                onValueChange = { weightText = it },
                                label = { Text("الوزن (جم)") },
                                placeholder = { Text("10.0", fontSize = 13.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = mfgCostText,
                                onValueChange = { mfgCostText = it },
                                label = { Text("المصنعية/جم") },
                                placeholder = { Text("95", fontSize = 13.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                    } else {
                        OutlinedTextField(
                            value = mfgCostText,
                            onValueChange = { mfgCostText = it },
                            label = { Text("السعر (ج.م)") },
                            placeholder = { Text("1500", fontSize = 13.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    OutlinedTextField(
                        value = descriptionText,
                        onValueChange = { descriptionText = it },
                        label = { Text("الوصف (اختياري)") },
                        placeholder = { Text(if (isGoldField) "مغلفة برقم تسلسلي" else "تفاصيل المنتج أو الخدمة", fontSize = 13.sp) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                // Equal-width Save & Cancel Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("إلغاء", fontWeight = FontWeight.SemiBold)
                    }

                    val canSave = title.isNotBlank() && if (isGoldField) {
                        (weightText.toDoubleOrNull() ?: 0.0) > 0
                    } else {
                        (mfgCostText.toDoubleOrNull() ?: 0.0) > 0
                    }
                    Button(
                        onClick = {
                            val weight = if (isGoldField) (weightText.toDoubleOrNull() ?: 0.0) else 1.0
                            val mfg = mfgCostText.toDoubleOrNull() ?: 0.0
                            if (canSave) {
                                val serializedMedia = FileUtils.serializeProductMedia(attachedMedia)
                                onConfirm(
                                    title.trim(),
                                    selectedCategory,
                                    selectedKarat,
                                    weight,
                                    mfg,
                                    serializedMedia,
                                    descriptionText.trim().takeIf { it.isNotBlank() }
                                )
                            }
                        },
                        enabled = canSave,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = adaptiveGoldPrimary,
                            contentColor = adaptiveOnPrimary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("حفظ", color = adaptiveOnPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    previewMediaItem?.let { preview ->
        MediaPreviewModal(
            item = preview,
            onDismiss = { previewMediaItem = null }
        )
    }
}

// -------------------------------------------------------------
// Component 8: Product Detail Dialog (With Views Counter + Multi-Industry Spec Adaptation)
// -------------------------------------------------------------
@Composable
fun ProductDetailDialog(
    product: MerchantProductEntity,
    goldPrices: GoldPricesModel,
    isGoldField: Boolean = true,
    fieldName: String = "المجوهرات والذهب",
    onDismiss: () -> Unit,
    onOrderClick: () -> Unit
) {
    val liveGramsPrice = when (product.karat.uppercase()) {
        "KARAT_24", "24K", "24" -> goldPrices.karat24
        "KARAT_18", "18K", "18" -> goldPrices.karat18
        "KARAT_14", "14K", "14" -> goldPrices.karat14
        "KARAT_999", "999" -> goldPrices.silver999
        "KARAT_925", "925" -> goldPrices.silver925
        "KARAT_800", "800" -> goldPrices.silver800
        else -> goldPrices.karat21
    }

    val totalCalculatedPrice = if (isGoldField) {
        (liveGramsPrice + product.manufacturingCostPerGram) * product.weightGrams
    } else {
        if (product.manufacturingCostPerGram > 0.0) {
            product.manufacturingCostPerGram * product.weightGrams.coerceAtLeast(1.0)
        } else {
            liveGramsPrice * product.weightGrams
        }
    }
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val mediaItems = remember(product.photoUri) { FileUtils.parseProductMedia(product.photoUri) }
    var activePreview by remember { mutableStateOf<MediaPreviewItem?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.93f)
                .navigationBarsPadding()
                .imePadding()
                .heightIn(max = 680.dp),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.45f))
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            imageVector = if (isGoldField) Icons.Default.Diamond else Icons.Default.Inventory2,
                            contentDescription = null,
                            tint = GoldPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(if (isGoldField) "مواصفات القطعة" else "تفاصيل المنتج", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = GoldPrimary)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("detail_product_views_badge_${product.id}")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Visibility, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(13.dp))
                                Text("${product.viewsCount} مشاهدة", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                        IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                if (mediaItems.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(mediaItems, key = { it.uri }) { item ->
                            Box(modifier = Modifier.size(100.dp)) {
                                OozoxThumbnailImage(
                                    model = item.uri,
                                    width = 100.dp,
                                    height = 100.dp,
                                    shape = RoundedCornerShape(12.dp),
                                    contentScale = ContentScale.Fit,
                                    contentDescription = product.title,
                                    onClick = {
                                        activePreview = MediaPreviewItem(
                                            uri = item.uri,
                                            caption = product.title,
                                            senderName = product.merchantShopName,
                                            isVideo = item.isVideo
                                        )
                                    }
                                )
                                if (item.isVideo) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .align(Alignment.Center)
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = 0.65f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = "تشغيل الفيديو", tint = Color.White, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                Text(product.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)

                val desc = product.description
                if (!desc.isNullOrBlank()) {
                    Text(desc, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Surface(
                    color = if (isDark) Color.Black.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (isGoldField) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("العيار", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                                Text(product.karat.replace("KARAT_", "عيار "), color = GoldPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("الوزن الصافي", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                                Text(GoldUtils.formatWeight(product.weightGrams), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("تكلفة المصنعية", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                                Text("${GoldUtils.formatCurrency(product.manufacturingCostPerGram)} / جرام", color = GoldAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), thickness = 0.8.dp)
                        } else {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("المجال", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                                Text(fieldName, color = GoldPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), thickness = 0.8.dp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(if (isGoldField) "الإجمالي التقديري" else "السعر", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                            Text(GoldUtils.formatCurrency(totalCalculatedPrice), color = GoldPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("إغلاق", fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = onOrderClick,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = adaptiveGoldPrimary, contentColor = adaptiveOnPrimary),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("طلب وشراء", color = adaptiveOnPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    activePreview?.let { preview ->
        MediaPreviewModal(
            item = preview,
            onDismiss = { activePreview = null }
        )
    }
}

// -------------------------------------------------------------
// Component 9: Merchant Inventory Overview Card
// -------------------------------------------------------------
@Composable
fun MerchantInventoryOverviewCard(
    totalStockWeight: Double,
    unclosedSales: List<MerchantDailySaleEntity>,
    onUpdateStockClick: () -> Unit,
    onRecordSaleClick: () -> Unit,
    onCloseReportClick: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val totalSoldTodayWeight = unclosedSales.sumOf { it.weightGrams }
    val totalRevenueTodayEgp = unclosedSales.sumOf { it.totalEGP }
    val totalProfitTodayEgp = unclosedSales.sumOf { it.profitEGP }
    val netRemainingWeight = (totalStockWeight - totalSoldTodayWeight).coerceAtLeast(0.0)

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.5.dp, Brush.horizontalGradient(listOf(GoldPrimary, GoldDark))),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(GoldDark.copy(alpha = 0.3f), CircleShape)
                            .border(1.dp, GoldPrimary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Scale, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("ميزان وجرد المحل المباشر", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text("خصم المبيعات المباشرة وإضافة الأرباح", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                IconButton(onClick = onUpdateStockClick) {
                    Icon(Icons.Default.EditNote, contentDescription = "تحديث المخزون", tint = GoldPrimary)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Weight Metrics Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Total Registered Stock
                Surface(
                    color = if (isDark) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("إجمالي الجرد الأصلي", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = GoldUtils.formatWeight(totalStockWeight),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Daily Sales Weight Deduction
                Surface(
                    color = if (isDark) Color(0xFF311B1B) else Color(0xFFFFECEC),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("خصم مبيعات اليوم", fontSize = 10.sp, color = if (isDark) Color(0xFFFCA5A5) else Color(0xFFDC2626))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "- ${GoldUtils.formatWeight(totalSoldTodayWeight)}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFEF4444)
                        )
                    }
                }

                // Net Inventory Available
                Surface(
                    color = if (isDark) Color(0xFF142E1F) else Color(0xFFECFDF5),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.5f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("الصافي بالمحل الآن", fontSize = 10.sp, color = if (isDark) Color(0xFF6EE7B7) else Color(0xFF059669))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = GoldUtils.formatWeight(netRemainingWeight),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Cash Revenue & Net Profit Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isDark) Color.Black.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("إجمالي المبيعات بالجنيه المصري", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = GoldUtils.formatCurrency(totalRevenueTodayEgp),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldPrimary
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("صافي الأرباح المضافة", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "+ ${GoldUtils.formatCurrency(totalProfitTodayEgp)}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981)
                    )
                }
            }

            if (unclosedSales.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onCloseReportClick,
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = adaptiveGoldPrimary, contentColor = adaptiveOnPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "اعتماد تقرير نهاية اليوم",
                        color = adaptiveOnPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Component 10: Merchant Sale Item Row
// -------------------------------------------------------------
@Composable
fun MerchantSaleItemRow(sale: MerchantDailySaleEntity) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    Surface(
        color = if (isDark) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
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
                    text = sale.productTitle,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("العميل: ${sale.customerName}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("الوزن: ${GoldUtils.formatWeight(sale.weightGrams)}", fontSize = 11.sp, color = GoldPrimary)
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = GoldUtils.formatCurrency(sale.totalEGP),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = GoldPrimary
                )
                Text(
                    text = "+ ${GoldUtils.formatCurrency(sale.profitEGP)} ربح",
                    fontSize = 11.sp,
                    color = Color(0xFF10B981),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// -------------------------------------------------------------
// Component 11: Merchant Closing Report Row
// -------------------------------------------------------------
@Composable
fun MerchantClosingReportRow(report: MerchantDailyClosingReportEntity) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    Surface(
        color = if (isDark) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Verified, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "إقفال يوم: ${report.reportDateText}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    color = GoldDark.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "${report.totalSalesCount} مبيعات",
                        fontSize = 10.sp,
                        color = GoldPrimary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("المباع: ${GoldUtils.formatWeight(report.totalWeightSoldGrams)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("الإيراد: ${GoldUtils.formatCurrency(report.totalRevenueEGP)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("الربح: +${GoldUtils.formatCurrency(report.totalProfitEGP)}", fontSize = 11.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
            }
        }
    }
}

// -------------------------------------------------------------
// Component 12: Update Stock Dialog
// -------------------------------------------------------------
@Composable
fun UpdateMerchantStockDialog(
    currentStock: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var stockInput by remember { mutableStateOf(currentStock.toString()) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .navigationBarsPadding()
                .imePadding(),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.35f))
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("تحديث رصيد الجرد", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = GoldPrimary)
                Text(
                    text = "سجل وزن الذهب الحالي (جرام) لتتبع المخزون.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = stockInput,
                    onValueChange = { stockInput = it },
                    label = { Text("الوزن (جرام)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("إلغاء", fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = {
                            val weight = stockInput.toDoubleOrNull() ?: currentStock
                            onConfirm(weight)
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = adaptiveGoldPrimary, contentColor = adaptiveOnPrimary),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("حفظ", color = adaptiveOnPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Component 13: Record Sale Dialog
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RecordMerchantSaleDialog(
    goldPrices: GoldPricesModel,
    onDismiss: () -> Unit,
    onConfirm: (
        title: String,
        grams: Double,
        karat: GoldKarat,
        priceGram: Double,
        mfgGram: Double,
        totalEgp: Double,
        profitEgp: Double,
        customerName: String,
        customerPhone: String?
    ) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedKarat by remember { mutableStateOf(GoldKarat.KARAT_21) }
    var weightText by remember { mutableStateOf("") }
    var mfgGramText by remember { mutableStateOf("100") }
    var customerName by remember { mutableStateOf("") }
    var customerPhone by remember { mutableStateOf("") }

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val basePriceGram = when (selectedKarat) {
        GoldKarat.KARAT_24 -> goldPrices.karat24
        GoldKarat.KARAT_21 -> goldPrices.karat21
        GoldKarat.KARAT_18 -> goldPrices.karat18
        GoldKarat.KARAT_14 -> goldPrices.karat14
    }

    val weight = weightText.toDoubleOrNull() ?: 0.0
    val mfgGram = mfgGramText.toDoubleOrNull() ?: 0.0
    val totalPerGram = basePriceGram + mfgGram
    val totalEgp = totalPerGram * weight
    val profitEgp = mfgGram * weight

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.93f)
                .navigationBarsPadding()
                .imePadding()
                .heightIn(max = 660.dp),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("تسجيل بيع مباشر", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = GoldPrimary)
                    IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("الصنف المباع") },
                        placeholder = { Text("خاتم عيار 18", fontSize = 13.sp) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Text("العيار", fontSize = 11.5.sp, color = GoldAccent, fontWeight = FontWeight.Bold)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        GoldKarat.entries.forEach { karat ->
                            FilterChip(
                                selected = selectedKarat == karat,
                                onClick = { selectedKarat = karat },
                                shape = RoundedCornerShape(10.dp),
                                label = { Text(karat.displayLabel(), fontSize = 11.5.sp) }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = weightText,
                            onValueChange = { weightText = it },
                            label = { Text("الوزن (جم)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = mfgGramText,
                            onValueChange = { mfgGramText = it },
                            label = { Text("المصنعية/جم") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    OutlinedTextField(
                        value = customerName,
                        onValueChange = { customerName = it },
                        label = { Text("اسم العميل") },
                        placeholder = { Text("اسم المشتري", fontSize = 13.sp) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Surface(
                        color = if (isDark) Color.Black.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("إجمالي القيمة", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(GoldUtils.formatCurrency(totalEgp), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = GoldPrimary)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("صافي الربح التقديري", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("+ ${GoldUtils.formatCurrency(profitEgp)}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("إلغاء", fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = {
                            if (title.isNotBlank() && weight > 0 && customerName.isNotBlank()) {
                                onConfirm(title, weight, selectedKarat, basePriceGram, mfgGram, totalEgp, profitEgp, customerName, customerPhone)
                            }
                        },
                        enabled = title.isNotBlank() && weight > 0 && customerName.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = adaptiveGoldPrimary, contentColor = adaptiveOnPrimary),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("حفظ", color = adaptiveOnPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Component 14: Close Daily Report Confirm Dialog
// -------------------------------------------------------------
@Composable
fun CloseDailyReportConfirmDialog(
    unclosedSales: List<MerchantDailySaleEntity>,
    currentStock: Double,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val totalWeightSold = unclosedSales.sumOf { it.weightGrams }
    val totalRevenue = unclosedSales.sumOf { it.totalEGP }
    val totalProfit = unclosedSales.sumOf { it.profitEGP }
    val remainingStock = (currentStock - totalWeightSold).coerceAtLeast(0.0)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .navigationBarsPadding()
                .imePadding(),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("اعتماد تقرير نهاية اليوم", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = adaptiveGoldPrimary)
                Text(
                    text = "سيتم إغلاق ${unclosedSales.size} عملية بيع وخصم ${GoldUtils.formatWeight(totalWeightSold)} من الجرد وترحيل ${GoldUtils.formatCurrency(totalProfit)} للأرشيف.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )

                Surface(
                    color = if (isDark) Color.Black.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("المتبقي بالجرد", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(GoldUtils.formatWeight(remainingStock), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = adaptiveStatusGreen)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("إجمالي الإيرادات", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(GoldUtils.formatCurrency(totalRevenue), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = adaptiveGoldPrimary)
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("إلغاء", fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = onConfirm,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = adaptiveGoldPrimary, contentColor = adaptiveOnPrimary),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("تأكيد الإقفال", color = adaptiveOnPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private fun String?.isNull_or_blank(): Boolean = this == null || this.trim().isEmpty()
