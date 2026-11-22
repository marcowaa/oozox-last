package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.theme.*
import com.example.util.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Enterprise Luxury Subscriptions, VIP Plans & Trial Controller
 * Clean layout, high-contrast hierarchy, 60-day interactive trial progression,
 * VIP plan cards, and one-tap request activation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionsScreen(
    currentUser: UserEntity?,
    subscriptionPlans: List<SubscriptionPlanEntity>,
    activeAnnouncements: List<PromoAnnouncementEntity>,
    onBackClick: () -> Unit,
    onSubmitSubscriptionRequest: (planId: String, planTitle: String, pricePaid: Double) -> Unit
) {
    var selectedPlanForRequest by remember { mutableStateOf<SubscriptionPlanEntity?>(null) }
    var showRequestDialog by remember { mutableStateOf(false) }
    var requestSuccessMsg by remember { mutableStateOf<String?>(null) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: الباقات والترقية, 1: حالة التجربة الحالية, 2: الميزات وخريطة التدرج

    val haptic = LocalHapticFeedback.current
    val subDetails = remember(currentUser?.subscriptionTier, currentUser?.subscriptionEndTimestamp, currentUser?.subscriptionStartTimestamp) {
        SubscriptionFeatureGate.evaluateSubscription(currentUser)
    }

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val primaryAccent = adaptiveGoldPrimary
    val onPrimaryAccent = adaptiveOnPrimary

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isDark) GoldPrimary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.WorkspacePremium,
                                contentDescription = null,
                                tint = primaryAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "إدارة الاشتراكات وباقات VIP 💎",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "القسم النشط: ${when (selectedTab) { 0 -> "باقات VIP والترقية"; 1 -> "حالة التجربة والاشتراك"; else -> "خريطة حجب المزايا" }}",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("subscriptions_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع",
                            tint = primaryAccent
                        )
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
            // Segmented Tab Selector
            Surface(
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val tabs = listOf(
                        "💎 باقات VIP" to 0,
                        "⏳ حالة الاشتراك" to 1,
                        "🗺️ حجب المزايا" to 2
                    )

                    tabs.forEach { (label, idx) ->
                        val isSelected = (selectedTab == idx)
                        val animatedElevation by animateDpAsState(
                            targetValue = if (isSelected) 3.dp else 0.dp,
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                            label = "sub_tab_elev_$idx"
                        )
                        val animatedBgColor by animateColorAsState(
                            targetValue = if (isSelected) primaryAccent else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            animationSpec = tween(220),
                            label = "sub_tab_bg_$idx"
                        )
                        val animatedBorderColor by animateColorAsState(
                            targetValue = if (isSelected) primaryAccent else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                            animationSpec = tween(220),
                            label = "sub_tab_border_$idx"
                        )

                        Surface(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedTab = idx
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = animatedBgColor,
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = animatedBorderColor
                            ),
                            shadowElevation = animatedElevation,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("sub_tab_$idx")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = onPrimaryAccent,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                    color = if (isSelected) onPrimaryAccent else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // Success Message Banner
            if (requestSuccessMsg != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = StatusGreen.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, StatusGreen.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusGreen, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = requestSuccessMsg!!,
                            fontSize = 12.5.sp,
                            color = StatusGreen,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { requestSuccessMsg = null },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = StatusGreen, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // Main Content Area
            AnimatedContent(
                targetState = selectedTab,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                label = "subscription_tabs_anim"
            ) { tabIdx ->
                when (tabIdx) {
                    0 -> PlansListTab(
                        subscriptionPlans = subscriptionPlans,
                        currentUser = currentUser,
                        subDetails = subDetails,
                        activeAnnouncements = activeAnnouncements,
                        onSelectPlan = { plan ->
                            selectedPlanForRequest = plan
                            showRequestDialog = true
                        },
                        haptic = haptic
                    )

                    1 -> TrialStatusTab(
                        currentUser = currentUser,
                        subDetails = subDetails
                    )

                    2 -> GradualFeatureRestrictionTab(
                        currentUser = currentUser,
                        haptic = haptic
                    )
                }
            }
        }
    }

    // Subscription Request Dialog
    if (showRequestDialog && selectedPlanForRequest != null) {
        val plan = selectedPlanForRequest!!
        val finalPrice = if (plan.isOfferActive && plan.discountPercent > 0) {
            plan.monthlyPrice * (1.0 - plan.discountPercent / 100.0)
        } else plan.monthlyPrice

        var notes by remember { mutableStateOf("") }
        var paymentMethod by remember { mutableStateOf("InstaPay / فودافون كاش") }

        AlertDialog(
            onDismissRequest = { showRequestDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Diamond, contentDescription = null, tint = GoldPrimary)
                    Text("طلب تفعيل باقة: ${plan.title}", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = GoldPrimary.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("السعر المطلوب سداده:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                "${finalPrice.toInt()} ج.م / شهرياً",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = adaptiveGoldLight
                            )
                        }
                    }

                    Text("طريقة التحويل أو الدفع المفضلة:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)

                    val methods = listOf("InstaPay / فودافون كاش", "تحويل بنكي مباشر", "الدفع عند المندوب / الصاغة")
                    methods.forEach { method ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { paymentMethod = method }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (paymentMethod == method),
                                onClick = { paymentMethod = method },
                                colors = RadioButtonDefaults.colors(selectedColor = GoldPrimary)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(method, fontSize = 12.5.sp)
                        }
                    }

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        placeholder = { Text("رقم المحفظة أو ملاحظات", fontSize = 12.sp) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { showRequestDialog = false },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("إلغاء", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = {
                            showRequestDialog = false
                            onSubmitSubscriptionRequest(plan.id, plan.title, finalPrice)
                            requestSuccessMsg = "تم إرسال طلب الاشتراك بنجاح! سيتم مراجعة وتفعيل الحساب فوراً."
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryAccent, contentColor = onPrimaryAccent),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("إرسال الطلب", fontWeight = FontWeight.Bold, color = onPrimaryAccent)
                    }
                }
            },
            dismissButton = {}
        )
    }
}

/**
 * Tab 0: Plans List with VIP Tiers
 */
@Composable
private fun PlansListTab(
    subscriptionPlans: List<SubscriptionPlanEntity>,
    currentUser: UserEntity?,
    subDetails: SubscriptionDetails,
    activeAnnouncements: List<PromoAnnouncementEntity>,
    onSelectPlan: (SubscriptionPlanEntity) -> Unit,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Promotional Offers (If any)
        if (activeAnnouncements.isNotEmpty()) {
            items(activeAnnouncements, key = { it.id }) { promo ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = StatusOrange.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, StatusOrange.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(StatusOrange.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.LocalOffer, contentDescription = null, tint = StatusOrange, modifier = Modifier.size(18.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(promo.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = StatusOrange)
                                if (promo.discountPercent > 0) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(color = StatusRed, shape = RoundedCornerShape(4.dp)) {
                                        Text("خصم ${promo.discountPercent}%", fontSize = 9.5.sp, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                    }
                                }
                            }
                            Text(promo.content, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        // Plans Loop
        items(subscriptionPlans, key = { it.id }) { plan ->
            val isCurrentPlan = (currentUser?.subscriptionTier == plan.id)
            val finalPrice = if (plan.isOfferActive && plan.discountPercent > 0) {
                plan.monthlyPrice * (1.0 - plan.discountPercent / 100.0)
            } else plan.monthlyPrice

            val isVip = (plan.id == SubscriptionTier.DIAMOND_VIP.name || plan.id == SubscriptionTier.GOLD_ENTERPRISE.name)
            val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
            val itemAccent = if (isVip) adaptiveGoldPrimary else MaterialTheme.colorScheme.primary
            val itemOnAccent = if (isVip) adaptiveOnPrimary else MaterialTheme.colorScheme.onPrimary

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(
                    width = if (isVip) 1.5.dp else 1.dp,
                    color = if (isVip) itemAccent else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                ),
                shadowElevation = if (isVip) 3.dp else 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isVip) (if (isDarkTheme) GoldPrimary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)) else MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isVip) Icons.Default.Diamond else Icons.Default.WorkspacePremium,
                                    contentDescription = null,
                                    tint = itemAccent,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = plan.title,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (!plan.badgeText.isNullOrBlank()) {
                                    Surface(
                                        color = if (isDarkTheme) GoldPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = plan.badgeText,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = itemAccent,
                                            maxLines = 1,
                                            softWrap = false,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Price Tag
                        Column(horizontalAlignment = Alignment.End) {
                            if (plan.monthlyPrice > 0) {
                                if (plan.isOfferActive && plan.discountPercent > 0) {
                                    Text(
                                        "${plan.monthlyPrice.toInt()} ج.م",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textDecoration = TextDecoration.LineThrough,
                                        softWrap = false
                                    )
                                }
                                Text(
                                    "${finalPrice.toInt()} ج.م",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Black,
                                    color = itemAccent,
                                    softWrap = false
                                )
                                Text("شهرياً", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, softWrap = false)
                            } else {
                                Text("مجاناً 100%", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = StatusGreen, softWrap = false)
                                Text("لمدة 60 يوم", fontSize = 10.sp, color = StatusGreen, softWrap = false)
                            }
                        }
                    }

                    Text(
                        text = plan.description,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    // Features Checklist
                    val features = plan.featuresListText.split("\n").filter { it.isNotBlank() }
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        features.forEach { feat ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = itemAccent, modifier = Modifier.size(15.dp))
                                Text(feat.trim(), fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }

                    // Action Button
                    if (isCurrentPlan && subDetails.phase == SubscriptionPhase.PAID_ACTIVE) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = adaptiveStatusGreen.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, adaptiveStatusGreen.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "باقتك الحالية المفعلة ⚡",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = adaptiveStatusGreen,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 10.dp)
                            )
                        }
                    } else if (plan.id == SubscriptionTier.FREE_TRIAL.name) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = adaptiveStatusGreen.copy(alpha = 0.1f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "مفعلة تلقائياً لجميع الحسابات الجديدة لمدة 60 يوماً ✅",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = adaptiveStatusGreen,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 10.dp)
                            )
                        }
                    } else {
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onSelectPlan(plan)
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = itemAccent
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        ) {
                            Icon(Icons.Default.CreditCard, contentDescription = null, tint = itemOnAccent, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "طلب تفعيل الاشتراك الآن 💎",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = itemOnAccent
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Tab 1: Current Subscription & Trial Status
 */
@Composable
private fun TrialStatusTab(
    currentUser: UserEntity?,
    subDetails: SubscriptionDetails
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Active Status Card
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.2.dp, if (subDetails.phase == SubscriptionPhase.PAID_ACTIVE) GoldPrimary else StatusGreen),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(StatusGreen.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Timer, contentDescription = null, tint = StatusGreen, modifier = Modifier.size(22.dp))
                            }
                            Column(modifier = Modifier.weight(1f, fill = false)) {
                                Text("حالة الحساب والاشتراك", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = subDetails.tier.label,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = adaptiveGoldLight,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = when (subDetails.phase) {
                                SubscriptionPhase.PAID_ACTIVE, SubscriptionPhase.TRIAL_ACTIVE -> StatusGreen.copy(alpha = 0.15f)
                                SubscriptionPhase.GRACE_WARNING_STAGE_1 -> StatusOrange.copy(alpha = 0.15f)
                                SubscriptionPhase.GRACE_WARNING_STAGE_2 -> Color(0xFFE11D48).copy(alpha = 0.15f)
                                SubscriptionPhase.BASE_RESTRICTED, SubscriptionPhase.PAID_EXPIRED -> StatusRed.copy(alpha = 0.15f)
                            }
                        ) {
                            Text(
                                text = when (subDetails.phase) {
                                    SubscriptionPhase.TRIAL_ACTIVE -> tr("free_trial_active", "تجربة مجانية نشطة 🎁")
                                    SubscriptionPhase.GRACE_WARNING_STAGE_1 -> tr("grace_stage_1", "تدرج: مرحلة 1 ⚠️")
                                    SubscriptionPhase.GRACE_WARNING_STAGE_2 -> tr("grace_stage_2", "تدرج: مرحلة 2 🚨")
                                    SubscriptionPhase.BASE_RESTRICTED -> tr("restricted", "حجب المزايا 🔒")
                                    SubscriptionPhase.PAID_ACTIVE -> tr("paid_active", "مشترك VIP 💎")
                                    SubscriptionPhase.PAID_EXPIRED -> tr("expired", "منتهي الصلاحية ❌")
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                color = when (subDetails.phase) {
                                    SubscriptionPhase.PAID_ACTIVE, SubscriptionPhase.TRIAL_ACTIVE -> StatusGreen
                                    SubscriptionPhase.GRACE_WARNING_STAGE_1 -> StatusOrange
                                    SubscriptionPhase.GRACE_WARNING_STAGE_2 -> Color(0xFFFB7185)
                                    SubscriptionPhase.BASE_RESTRICTED, SubscriptionPhase.PAID_EXPIRED -> StatusRed
                                },
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = subDetails.alertMessage,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )

                    if (subDetails.isTrial) {
                        Spacer(modifier = Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { subDetails.trialProgressRatio },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = StatusGreen,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("اليوم 1", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("متبقي ${subDetails.trialDaysRemaining} من 60 يوماً", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = StatusGreen)
                            Text("اليوم 60", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

    }
}

/**
 * Tab 2: Gradual Restriction Roadmap & Feature Matrix
 */
@Composable
private fun GradualFeatureRestrictionTab(
    currentUser: UserEntity?,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Roadmap Timeline Card
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        "خريطة التدرج الزمني في حجب المزايا (60 يوماً):",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = adaptiveGoldLight
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    val stages = listOf(
                        Triple("الأيام 1 إلى 60 (التجربة المجانية):", "فترة شهرين مجاناً تشمل المحاسب المالي، الخزائن المشفرة، وموازين الصاغة بلا أي قيود.", StatusGreen),
                        Triple("الأيام 61 إلى 70 (تدرج مرحلة 1):", "حجب ميزات الحسابات المتقدمة والمزامنة السحابية مع تنبيه مستمر.", StatusOrange),
                        Triple("الأيام 71 إلى 80 (تدرج مرحلة 2):", "حجب تصدير تقارير Excel، إنشاء الخزائن السرية الجديدة، وتدقيق العجز.", Color(0xFFFB7185)),
                        Triple("بعد اليوم 80 (الحجب الشامل):", "قصر التطبيق على الاستعراض والرسائل الأساسية فقط حتى الاشتراك.", StatusRed)
                    )

                    stages.forEach { (title, desc, color) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(color)
                            )
                            Column {
                                Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color)
                                Text(desc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 15.sp)
                            }
                        }
                    }
                }
            }
        }

        // Live Feature Access Matrix
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        "حالة الميزات بحسابك الآن:",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val features = AppFeature.entries
                    features.forEach { feat ->
                        val access = SubscriptionFeatureGate.checkFeatureAccess(currentUser, feat)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val currentLang = com.example.util.LocalAppLanguage.current
                            Column(modifier = Modifier.weight(1f)) {
                                Text(feat.localizedTitle(currentLang), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                Text(feat.localizedDescription(currentLang), fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (access.isAllowed) StatusGreen.copy(alpha = 0.15f) else StatusRed.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = if (access.isAllowed) "متاح ✅" else "مقيد 🔒",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (access.isAllowed) StatusGreen else StatusRed,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                    }
                }
            }
        }
    }
}
