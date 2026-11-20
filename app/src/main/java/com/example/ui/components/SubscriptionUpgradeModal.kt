package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
    val context = LocalContext.current
    val subDetails = remember(currentUser?.subscriptionTier, currentUser?.subscriptionEndTimestamp, currentUser?.subscriptionStartTimestamp) {
        SubscriptionFeatureGate.evaluateSubscription(currentUser)
    }

    val plans = remember(subscriptionPlans) {
        if (subscriptionPlans.isNotEmpty()) {
            subscriptionPlans
        } else {
            listOf(
                SubscriptionPlanEntity(
                    id = SubscriptionTier.FREE_TRIAL.name,
                    title = "الفترة التجريبية المجانية",
                    badgeText = "60 يوماً مجاناً",
                    monthlyPrice = 0.0,
                    annualPrice = 0.0,
                    description = "تشمل كل مزايا المنصة المتطورة لمدة شهرين كاملين بدون أي قيود.",
                    featuresListText = "• المساعد المالي الذكي Gemini AI\n• التشفير الآمن للخزائن السرية\n• المزامنة السحابية الاحتياطية\n• رادار كشف التجار القريبين\n• تقارير الأرباح وكشوف الحسابات",
                    isPopular = false
                ),
                SubscriptionPlanEntity(
                    id = SubscriptionTier.SILVER_TRADER.name,
                    title = "باقة التاجر الفضية",
                    badgeText = "الأكثر مرونة",
                    monthlyPrice = 499.0,
                    annualPrice = 4990.0,
                    description = "مثالية للتجار الأفراد والورش الصغيرة لإدارة الحسابات وفواتير الذهب.",
                    featuresListText = "• جميع مزايا الفترة التجريبية بدون انقطاع\n• تقارير Excel/PDF غير محدودة\n• دعم فني مخصص للتجار\n• استخراج كشوف حسابات العملاء",
                    isPopular = false
                ),
                SubscriptionPlanEntity(
                    id = SubscriptionTier.GOLD_ENTERPRISE.name,
                    title = "باقة الذهب للشركات والمؤسسات",
                    badgeText = "الأكثر شعبية",
                    monthlyPrice = 999.0,
                    annualPrice = 9990.0,
                    description = "الباقة القياسية المتكاملة لكبرى محلات الصاغة ومكاتب تجارة المعادن الثمينة.",
                    featuresListText = "• سعة حسابات ومحادثات غير محدودة\n• تحليل الصفقات والموازين بالذكاء الاصطناعي\n• ربط خزائن متعددة وتشفير سحابي دائم\n• أولوية قصوى في عمليات التقاص والتوثيق",
                    isPopular = true
                ),
                SubscriptionPlanEntity(
                    id = SubscriptionTier.DIAMOND_VIP.name,
                    title = "باقة الماس لكبار الصاغة VIP",
                    badgeText = "نخبة التجار",
                    monthlyPrice = 1899.0,
                    annualPrice = 18990.0,
                    description = "أعلى درجات الموثوقية والدعم لكبار المستثمرين ومصانع الذهب والسبائك.",
                    featuresListText = "• خادم سحابي خاص فائق السرعة ومزامنة فورية\n• تدقيق مالي محاسبي شامل واستشارات الصاغة\n• شارة توثيق ماسية مميزة تظهر لجميع التجار\n• دعم هاتفي مباشر 24/7",
                    isPopular = false
                )
            )
        }
    }

    var selectedPlanForUpgrade by remember { mutableStateOf<SubscriptionPlanEntity?>(null) }
    var showPaymentConfirmDialog by remember { mutableStateOf(false) }
    var successToastMessage by remember { mutableStateOf<String?>(null) }
    var isAnnualBilling by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()) }

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
                    Row(verticalAlignment = Alignment.CenterVertically) {
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
                        Column {
                            Text(
                                text = "باقات واشتراكات OOZOX للأعمال",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "عروض الترقية، إدارة الأعمال وحالة باقتك الحالية",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                                text = "الباقة المشترك بها حالياً",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = subDetails.tier.label,
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = GoldPrimary,
                                                maxLines = 1,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
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
                                                SubscriptionPhase.PAID_ACTIVE -> tr("paid_active", "مشترك VIP 💎")
                                                SubscriptionPhase.TRIAL_ACTIVE -> tr("free_trial", "تجربة مجانية")
                                                SubscriptionPhase.GRACE_WARNING_STAGE_1 -> tr("grace_stage_1", "تدرج 1 ⚠️")
                                                SubscriptionPhase.GRACE_WARNING_STAGE_2 -> tr("grace_stage_2", "تدرج 2 🚨")
                                                SubscriptionPhase.BASE_RESTRICTED -> tr("restricted", "حجب 🔒")
                                                SubscriptionPhase.PAID_EXPIRED -> tr("expired", "منتهي ❌")
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
                                            text = "متبقي ${subDetails.trialDaysRemaining} من 60 يوم",
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
                                                    text = "تنبيه: اقتراب انتهاء الفترة التجريبية! قم بالترقية الآن لتجنب تقييد الذكاء الاصطناعي وتقارير الصاغة.",
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
                                                    "المرحلة 1: تم تقييد الذكاء الاصطناعي والمزامنة السحابية. سارع بالترقية لتفادي تقييد كشوف الحسابات!"
                                                else
                                                    "المرحلة 2: تم تقييد الخزائن والتقارير المالية. يرجى الاشتراك في باقة مدفوعة لفتح كافة المزايا.",
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
                                Text("العروض والخصومات المتاحة حالياً", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = GoldPrimary)
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
                                                                "خصم ${promo.discountPercent}%",
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
                                                    Text("كوبون الخصم: ${promo.promoCode}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = adaptiveGoldPrimary)
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
                                                Text("عرض الترقية السنوية للصاغة", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    color = StatusGreen,
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text("خصم 20%", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                                }
                                            }
                                            Text("احصل على خصم 20% إضافي وشهرين مجاناً عند تفعيل الاشتراك السنوي لأي باقة.", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text("الكود الترويجي: GOLDVIP2026", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = GoldPrimary)
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
                                Column {
                                    Text("نوع دورة الفوترة:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = if (isAnnualBilling) "اشتراك سنوي (خصم 20% إضافي )" else "اشتراك شهري مرن",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isAnnualBilling) StatusGreen else GoldPrimary
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("شهري", fontSize = 11.sp, color = if (!isAnnualBilling) GoldPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                                    Switch(
                                        checked = isAnnualBilling,
                                        onCheckedChange = { isAnnualBilling = it },
                                        modifier = Modifier.padding(horizontal = 6.dp)
                                    )
                                    Text("سنوي", fontSize = 11.sp, color = if (isAnnualBilling) StatusGreen else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // 4. Subscription Plans for Direct Upgrade / Renewal
                    item {
                        Text(
                            text = "الباقات المتاحة للترقية أو التجديد",
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
                                                    text = if (isAnnualBilling) "${(plan.monthlyPrice * 12).toInt()} ج.م" else "${plan.monthlyPrice.toInt()} ج.م",
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    textDecoration = TextDecoration.LineThrough,
                                                    softWrap = false
                                                )
                                            }
                                            Text(
                                                text = if (isAnnualBilling) "${finalPrice.toInt()} ج.م / سنة" else "${finalPrice.toInt()} ج.م / شهر",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = GoldPrimary,
                                                softWrap = false
                                            )
                                        } else {
                                            Text(
                                                text = "مجاناً 100%",
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
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusGreen, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("باقتك الحالية المفعلة", fontSize = 12.sp, color = StatusGreen, fontWeight = FontWeight.Bold)
                                    }
                                } else if (plan.id == SubscriptionTier.FREE_TRIAL.name) {
                                    Surface(
                                        color = StatusGreen.copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "مفعلة تلقائياً لجميع الحسابات الجديدة لمدة 60 يوماً كاملة",
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
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            imageVector = if (subDetails.phase == SubscriptionPhase.PAID_ACTIVE) Icons.Default.Upgrade else Icons.Default.RocketLaunch,
                                            contentDescription = null,
                                            tint = Color.Black,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (subDetails.phase == SubscriptionPhase.PAID_ACTIVE) "ترقية إلى هذه الباقة" else "اشتراك وترقية فورية",
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
                                .clickable {
                                    onDismiss()
                                    onOpenFullSubscriptionsScreen()
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Tune, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("محاكي التدرج وخريطة الميزات الكاملة ↗️", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GoldPrimary)
                                        Text("فتح صفحة إدارة الاشتراكات المتقدمة", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                    imageVector = Icons.Default.CreditCard,
                                    contentDescription = null,
                                    tint = GoldPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = "تأكيد طلب الترقية",
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
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "إغلاق",
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
                        Text(
                            text = "المبلغ الإجمالي المطلوب: ${calculatedPrice.toInt()} ج.م ${if (isAnnualBilling) "(سنوياً)" else "(شهرياً)"}",
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
                                Text("طرق السداد المعتمدة:", fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurface)
                                Text("• InstaPay / المحافظ الإلكترونية / التحويل البنكي وفقاً لبيانات الدفع الصادرة من الإدارة.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        OutlinedTextField(
                            value = paymentPhoneOrRef,
                            onValueChange = { paymentPhoneOrRef = it },
                            label = { Text("رقم الهاتف أو رقم مرجع عملية التحويل", fontSize = 11.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            text = "بمجرد الضغط على تأكيد، يتم إرسال إشعار فوري لمراجعة التحويل وتفعيل الباقة وتحديث صلاحيات حسابك.",
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
                            Text("إلغاء", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = {
                                isSubmitting = true
                                onSubmitSubscriptionRequest(plan.id, plan.title, calculatedPrice)
                                showPaymentConfirmDialog = false
                                successToastMessage = "تم إرسال طلب ترقية حسابك إلى باقة «${plan.title}» بنجاح! سيتم مراجعة الطلب وتفعيل الميزات فوراً."
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = MaterialTheme.colorScheme.background),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                        ) {
                            Text("تأكيد الترقية", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}
