package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.*
import com.example.ui.theme.*
import com.example.util.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionUpgradeModal(
    currentUser: UserEntity?,
    subscriptionPlans: List<SubscriptionPlanEntity>,
    activeAnnouncements: List<PromoAnnouncementEntity>,
    onDismiss: () -> Unit,
    onSubmitSubscriptionRequest: (planId: String, planTitle: String, pricePaid: Double) -> Unit,
    onOpenFullSubscriptionsScreen: () -> Unit
) {
    val currentLanguage = LocalAppLanguage.current
    @Suppress("UNUSED_VARIABLE")
    val catalogTick = Translations.catalogVersion

    val subDetails = remember(
        currentUser?.subscriptionTier,
        currentUser?.subscriptionEndTimestamp,
        currentUser?.subscriptionStartTimestamp,
        currentLanguage,
        catalogTick
    ) {
        SubscriptionFeatureGate.evaluateSubscription(currentUser, currentLanguage)
    }

    val plans = remember(subscriptionPlans, currentLanguage, catalogTick) {
        if (subscriptionPlans.isNotEmpty()) {
            subscriptionPlans
        } else {
            listOf(
                SubscriptionPlanEntity(
                    id = SubscriptionTier.FREE_TRIAL.name,
                    title = SubscriptionTier.FREE_TRIAL.localizedLabel(currentLanguage),
                    badgeText = SubscriptionTier.FREE_TRIAL.localizedBadgeText(currentLanguage),
                    monthlyPrice = 0.0,
                    annualPrice = 0.0,
                    description = SubscriptionTier.FREE_TRIAL.localizedDescription(currentLanguage),
                    featuresListText = Translations.get("sub.plan_free_features", currentLanguage),
                    isPopular = false
                ),
                SubscriptionPlanEntity(
                    id = SubscriptionTier.SILVER_TRADER.name,
                    title = SubscriptionTier.SILVER_TRADER.localizedLabel(currentLanguage),
                    badgeText = SubscriptionTier.SILVER_TRADER.localizedBadgeText(currentLanguage),
                    monthlyPrice = 499.0,
                    annualPrice = 4990.0,
                    description = SubscriptionTier.SILVER_TRADER.localizedDescription(currentLanguage),
                    featuresListText = Translations.get("sub.plan_silver_features", currentLanguage),
                    isPopular = false
                ),
                SubscriptionPlanEntity(
                    id = SubscriptionTier.GOLD_ENTERPRISE.name,
                    title = SubscriptionTier.GOLD_ENTERPRISE.localizedLabel(currentLanguage),
                    badgeText = SubscriptionTier.GOLD_ENTERPRISE.localizedBadgeText(currentLanguage),
                    monthlyPrice = 999.0,
                    annualPrice = 9990.0,
                    description = SubscriptionTier.GOLD_ENTERPRISE.localizedDescription(currentLanguage),
                    featuresListText = Translations.get("sub.plan_gold_features", currentLanguage),
                    isPopular = true
                ),
                SubscriptionPlanEntity(
                    id = SubscriptionTier.DIAMOND_VIP.name,
                    title = SubscriptionTier.DIAMOND_VIP.localizedLabel(currentLanguage),
                    badgeText = SubscriptionTier.DIAMOND_VIP.localizedBadgeText(currentLanguage),
                    monthlyPrice = 1899.0,
                    annualPrice = 18990.0,
                    description = SubscriptionTier.DIAMOND_VIP.localizedDescription(currentLanguage),
                    featuresListText = Translations.get("sub.plan_diamond_features", currentLanguage),
                    isPopular = false
                )
            )
        }
    }

    var selectedPlanForUpgrade by remember { mutableStateOf<SubscriptionPlanEntity?>(null) }
    var showPaymentConfirmDialog by remember { mutableStateOf(false) }
    var successToastMessage by remember { mutableStateOf<String?>(null) }
    var isAnnualBilling by remember { mutableStateOf(false) }

    remember(currentLanguage) { SimpleDateFormat("yyyy/MM/dd", Locale(currentLanguage.code)) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .testTag("subscription_upgrade_modal"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.2.dp, GoldPrimary.copy(alpha = 0.55f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                // Modal Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = GoldPrimary.copy(alpha = 0.2f),
                            shape = CircleShape,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.WorkspacePremium,
                                    contentDescription = null,
                                    tint = GoldPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tr("sub.modal_title"),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = tr("sub.modal_sub"),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("close_upgrade_modal_btn")
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = tr("common.close"),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(10.dp))

                // Success Message Banner
                AnimatedVisibility(
                    visible = successToastMessage != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    successToastMessage?.let { msg ->
                        Surface(
                            color = StatusGreen.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, StatusGreen),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusGreen, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(msg, fontSize = 12.sp, color = StatusGreen, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Scrollable Content
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 1. Current Active Subscribed Plan Card
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = when (subDetails.phase) {
                                    SubscriptionPhase.PAID_ACTIVE -> GoldDark.copy(alpha = 0.25f)
                                    SubscriptionPhase.TRIAL_ACTIVE -> Color(0xFF064E3B).copy(alpha = 0.25f)
                                    SubscriptionPhase.GRACE_WARNING_STAGE_1 -> Color(0xFF7C2D12).copy(alpha = 0.25f)
                                    SubscriptionPhase.GRACE_WARNING_STAGE_2 -> Color(0xFF881337).copy(alpha = 0.25f)
                                    SubscriptionPhase.BASE_RESTRICTED, SubscriptionPhase.PAID_EXPIRED -> Color(0xFF450A0A).copy(alpha = 0.3f)
                                }
                            ),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(
                                1.5.dp,
                                when (subDetails.phase) {
                                    SubscriptionPhase.PAID_ACTIVE -> GoldPrimary
                                    SubscriptionPhase.TRIAL_ACTIVE -> StatusGreen
                                    SubscriptionPhase.GRACE_WARNING_STAGE_1 -> StatusOrange
                                    SubscriptionPhase.GRACE_WARNING_STAGE_2 -> Color(0xFFFB7185)
                                    SubscriptionPhase.BASE_RESTRICTED, SubscriptionPhase.PAID_EXPIRED -> StatusRed
                                }
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = when (subDetails.phase) {
                                                SubscriptionPhase.PAID_ACTIVE -> Icons.Default.Diamond
                                                SubscriptionPhase.TRIAL_ACTIVE -> Icons.Default.Verified
                                                SubscriptionPhase.GRACE_WARNING_STAGE_1 -> Icons.Default.WarningAmber
                                                SubscriptionPhase.GRACE_WARNING_STAGE_2 -> Icons.Default.ReportProblem
                                                SubscriptionPhase.BASE_RESTRICTED -> Icons.Default.Lock
                                                SubscriptionPhase.PAID_EXPIRED -> Icons.Default.TimerOff
                                            },
                                            contentDescription = null,
                                            tint = when (subDetails.phase) {
                                                SubscriptionPhase.PAID_ACTIVE -> GoldPrimary
                                                SubscriptionPhase.TRIAL_ACTIVE -> StatusGreen
                                                SubscriptionPhase.GRACE_WARNING_STAGE_1 -> StatusOrange
                                                SubscriptionPhase.GRACE_WARNING_STAGE_2 -> Color(0xFFFB7185)
                                                SubscriptionPhase.BASE_RESTRICTED, SubscriptionPhase.PAID_EXPIRED -> StatusRed
                                            },
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f, fill = false)) {
                                            Text(
                                                text = tr("sub.current_plan"),
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = subDetails.tier.localizedLabel(currentLanguage),
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = GoldPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Surface(
                                        color = when (subDetails.phase) {
                                            SubscriptionPhase.PAID_ACTIVE, SubscriptionPhase.TRIAL_ACTIVE -> StatusGreen.copy(alpha = 0.2f)
                                            SubscriptionPhase.GRACE_WARNING_STAGE_1 -> StatusOrange.copy(alpha = 0.2f)
                                            SubscriptionPhase.GRACE_WARNING_STAGE_2 -> Color(0xFFE11D48).copy(alpha = 0.2f)
                                            SubscriptionPhase.BASE_RESTRICTED, SubscriptionPhase.PAID_EXPIRED -> StatusRed.copy(alpha = 0.2f)
                                        },
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = when (subDetails.phase) {
                                                SubscriptionPhase.PAID_ACTIVE -> tr("sub.badge_paid_active")
                                                SubscriptionPhase.TRIAL_ACTIVE -> tr("sub.badge_free_trial")
                                                SubscriptionPhase.GRACE_WARNING_STAGE_1 -> tr("sub.badge_grace_1")
                                                SubscriptionPhase.GRACE_WARNING_STAGE_2 -> tr("sub.badge_grace_2")
                                                SubscriptionPhase.BASE_RESTRICTED -> tr("sub.badge_restricted")
                                                SubscriptionPhase.PAID_EXPIRED -> tr("sub.badge_expired")
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

                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = subDetails.alertMessage,
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = 16.sp
                                )

                                if (subDetails.isTrial && subDetails.phase == SubscriptionPhase.TRIAL_ACTIVE) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        LinearProgressIndicator(
                                            progress = { subDetails.trialProgressRatio },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(8.dp)
                                                .clip(RoundedCornerShape(4.dp)),
                                            color = if (subDetails.trialDaysRemaining <= 10) StatusOrange else StatusGreen,
                                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        Text(
                                            text = trFmt("sub.trial_progress_fmt", "days" to subDetails.trialDaysRemaining),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (subDetails.trialDaysRemaining <= 10) StatusOrange else StatusGreen
                                        )
                                    }

                                    if (subDetails.trialDaysRemaining <= 15) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Surface(
                                            color = StatusOrange.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, StatusOrange.copy(alpha = 0.5f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.HourglassTop, contentDescription = null, tint = StatusOrange, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = tr("sub.trial_warn_soon"),
                                                    fontSize = 10.5.sp,
                                                    color = StatusOrange,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }
                                } else if (subDetails.phase == SubscriptionPhase.GRACE_WARNING_STAGE_1 || subDetails.phase == SubscriptionPhase.GRACE_WARNING_STAGE_2) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Surface(
                                        color = StatusRed.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, StatusRed.copy(alpha = 0.5f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Warning, contentDescription = null, tint = StatusRed, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (subDetails.phase == SubscriptionPhase.GRACE_WARNING_STAGE_1)
                                                    tr("sub.stage1_banner_warn")
                                                else
                                                    tr("sub.stage2_banner_warn"),
                                                fontSize = 10.5.sp,
                                                color = StatusRed,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 2. Active Special Offers & Discounts Section
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = StatusOrange, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(tr("sub.announcements_title"), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = GoldPrimary)
                            }

                            if (activeAnnouncements.isNotEmpty()) {
                                activeAnnouncements.forEach { promo ->
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, StatusOrange.copy(alpha = 0.6f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                color = StatusOrange.copy(alpha = 0.2f),
                                                shape = CircleShape,
                                                modifier = Modifier.size(34.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(Icons.Default.Percent, contentDescription = null, tint = StatusOrange, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(promo.title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                                    if (promo.discountPercent > 0) {
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Surface(
                                                            color = StatusRed,
                                                            shape = RoundedCornerShape(4.dp)
                                                        ) {
                                                            Text(
                                                                trFmt("sub.discount_badge_fmt", "percent" to promo.discountPercent),
                                                                color = Color.White,
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                                Text(promo.content, fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                if (promo.promoCode != null) {
                                                    Text(
                                                        trFmt("sub.promo_code_fmt", "code" to promo.promoCode),
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = adaptiveGoldPrimary
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                // Standard Active Promotional Banner
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            color = GoldPrimary.copy(alpha = 0.2f),
                                            shape = CircleShape,
                                            modifier = Modifier.size(34.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.CardGiftcard, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(tr("sub.promo_default_title"), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    color = StatusGreen,
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(
                                                        tr("sub.promo_default_badge"),
                                                        color = Color.Black,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                            Text(tr("sub.promo_default_desc"), fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(
                                                trFmt("sub.promo_code_fmt", "code" to "GOLDVIP2026"),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = GoldPrimary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 3. Billing Toggle (Monthly vs Annual with 20% Extra Discount)
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(tr("sub.billing_cycle_label"), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = if (isAnnualBilling) tr("sub.billing_annual_full") else tr("sub.billing_monthly_full"),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isAnnualBilling) StatusGreen else GoldPrimary
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        tr("sub.billing_month_short"),
                                        fontSize = 11.sp,
                                        color = if (!isAnnualBilling) GoldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Switch(
                                        checked = isAnnualBilling,
                                        onCheckedChange = { isAnnualBilling = it },
                                        modifier = Modifier
                                            .padding(horizontal = 6.dp)
                                            .testTag("billing_cycle_switch")
                                    )
                                    Text(
                                        tr("sub.billing_year_short"),
                                        fontSize = 11.sp,
                                        color = if (isAnnualBilling) StatusGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // 4. Subscription Plans for Direct Upgrade / Renewal
                    item {
                        Text(
                            text = tr("sub.available_plans_title"),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldPrimary
                        )
                    }

                    items(plans) { plan ->
                        val isCurrentPlan = currentUser?.subscriptionTier == plan.id && subDetails.phase == SubscriptionPhase.PAID_ACTIVE
                        val baseMonthly = plan.monthlyPrice
                        val discountedMonthly = if (plan.isOfferActive && plan.discountPercent > 0) {
                            baseMonthly * (1.0 - plan.discountPercent / 100.0)
                        } else baseMonthly

                        val finalPrice = if (isAnnualBilling && discountedMonthly > 0) {
                            discountedMonthly * 0.8 * 12 // 20% extra discount on annual
                        } else {
                            discountedMonthly
                        }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(
                                width = if (plan.isPopular || plan.id == SubscriptionTier.DIAMOND_VIP.name) 2.dp else 1.dp,
                                color = when (plan.id) {
                                    SubscriptionTier.FREE_TRIAL.name -> adaptiveStatusOrange
                                    SubscriptionTier.SILVER_TRADER.name -> adaptivePlatinumSilver
                                    SubscriptionTier.GOLD_ENTERPRISE.name -> adaptiveGoldPrimary
                                    SubscriptionTier.DIAMOND_VIP.name -> adaptiveGoldLight
                                    else -> MaterialTheme.colorScheme.outlineVariant
                                }
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = plan.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (plan.badgeText != null) {
                                            Surface(
                                                color = GoldPrimary.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = plan.badgeText,
                                                    fontSize = 9.5.sp,
                                                    color = GoldPrimary,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    softWrap = false,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        if (plan.offerTitle != null && plan.isOfferActive) {
                                            Text(plan.offerTitle, fontSize = 10.sp, color = StatusOrange, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        if (plan.monthlyPrice > 0) {
                                            if (plan.isOfferActive && plan.discountPercent > 0) {
                                                Text(
                                                    text = GoldUtils.formatCurrency(
                                                        if (isAnnualBilling) plan.monthlyPrice * 12 else plan.monthlyPrice,
                                                        currentLanguage
                                                    ),
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    textDecoration = TextDecoration.LineThrough,
                                                    softWrap = false
                                                )
                                            }
                                            val formattedFinalPrice = GoldUtils.formatCurrency(finalPrice, currentLanguage)
                                            Text(
                                                text = if (isAnnualBilling)
                                                    trFmt("sub.price_per_year_fmt", "price" to formattedFinalPrice)
                                                else
                                                    trFmt("sub.price_per_month_fmt", "price" to formattedFinalPrice),
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = GoldPrimary,
                                                softWrap = false
                                            )
                                        } else {
                                            Text(
                                                text = tr("sub.free_100"),
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = StatusGreen,
                                                softWrap = false
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = plan.description,
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 16.sp
                                )

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                                // Features Preview
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    plan.featuresListText.split("\n").take(4).forEach { feat ->
                                        if (feat.isNotBlank()) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Check, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(13.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(feat.trim(), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                                            }
                                        }
                                    }
                                }

                                if (isCurrentPlan) {
                                    OutlinedButton(
                                        onClick = { },
                                        enabled = false,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 48.dp),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusGreen, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(tr("sub.active_current_btn"), fontSize = 12.sp, color = StatusGreen, fontWeight = FontWeight.Bold)
                                    }
                                } else if (plan.id == SubscriptionTier.FREE_TRIAL.name) {
                                    Surface(
                                        color = StatusGreen.copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = tr("sub.trial_auto_active"),
                                            fontSize = 10.5.sp,
                                            color = StatusGreen,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(8.dp),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                } else {
                                    Button(
                                        onClick = {
                                            selectedPlanForUpgrade = plan
                                            showPaymentConfirmDialog = true
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (plan.id == SubscriptionTier.DIAMOND_VIP.name) GoldAccent else GoldPrimary
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 48.dp)
                                            .testTag("upgrade_plan_btn_${plan.id}")
                                    ) {
                                        Icon(
                                            imageVector = if (subDetails.phase == SubscriptionPhase.PAID_ACTIVE) Icons.Default.Upgrade else Icons.Default.RocketLaunch,
                                            contentDescription = null,
                                            tint = Color.Black,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (subDetails.phase == SubscriptionPhase.PAID_ACTIVE)
                                                tr("sub.upgrade_to_plan")
                                            else
                                                tr("sub.subscribe_now"),
                                            color = Color.Black,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.5.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 5. Open Full Subscriptions Page Shortcut
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .clickable {
                                    onDismiss()
                                    onOpenFullSubscriptionsScreen()
                                }
                                .testTag("open_full_subscriptions_card")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Tune, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(tr("sub.open_simulator_title"), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GoldPrimary)
                                        Text(tr("sub.open_simulator_sub"), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    // Payment and Upgrade Confirmation Dialog
    if (showPaymentConfirmDialog && selectedPlanForUpgrade != null) {
        val plan = selectedPlanForUpgrade!!
        val baseMonthly = plan.monthlyPrice
        val discountedMonthly = if (plan.isOfferActive && plan.discountPercent > 0) {
            baseMonthly * (1.0 - plan.discountPercent / 100.0)
        } else baseMonthly

        val calculatedPrice = if (isAnnualBilling && discountedMonthly > 0) {
            discountedMonthly * 0.8 * 12
        } else {
            discountedMonthly
        }

        var paymentPhoneOrRef by remember { mutableStateOf(currentUser?.phone ?: "") }
        var isSubmitting by remember { mutableStateOf(false) }

        Dialog(
            onDismissRequest = { if (!isSubmitting) showPaymentConfirmDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .wrapContentHeight()
                    .testTag("subscription_payment_confirm_dialog"),
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
                            modifier = Modifier.weight(1f),
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
                                    imageVector = Icons.Default.CreditCard,
                                    contentDescription = null,
                                    tint = GoldPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = tr("sub.confirm_upgrade_title"),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = plan.title,
                                    fontSize = 11.sp,
                                    color = GoldPrimary
                                )
                            }
                        }

                        IconButton(
                            onClick = { if (!isSubmitting) showPaymentConfirmDialog = false },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = tr("common.close"),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                        thickness = 0.8.dp
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        val cycleText = if (isAnnualBilling) tr("sub.cycle_annual_paren") else tr("sub.cycle_monthly_paren")
                        val formattedTotal = GoldUtils.formatCurrency(calculatedPrice, currentLanguage)
                        Text(
                            text = trFmt("sub.total_due_fmt", "price" to formattedTotal, "cycle" to cycleText),
                            fontWeight = FontWeight.Bold,
                            color = GoldPrimary,
                            fontSize = 13.sp
                        )

                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(tr("sub.payment_methods_title"), fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurface)
                                Text(tr("sub.payment_methods_desc"), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        OutlinedTextField(
                            value = paymentPhoneOrRef,
                            onValueChange = { paymentPhoneOrRef = it },
                            label = { Text(tr("sub.transfer_ref_label"), fontSize = 11.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("upgrade_transfer_ref_input")
                        )

                        Text(
                            text = tr("sub.confirm_upgrade_note"),
                            fontSize = 10.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 15.sp
                        )
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
                            onClick = { if (!isSubmitting) showPaymentConfirmDialog = false },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                        ) {
                            Text(tr("common.cancel"), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = {
                                isSubmitting = true
                                onSubmitSubscriptionRequest(plan.id, plan.title, calculatedPrice)
                                showPaymentConfirmDialog = false
                                successToastMessage = Translations.format("sub.req_success_fmt", currentLanguage, "plan" to plan.title)
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = MaterialTheme.colorScheme.background),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("confirm_upgrade_submit_btn")
                        ) {
                            Text(tr("sub.confirm_upgrade_btn"), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}
