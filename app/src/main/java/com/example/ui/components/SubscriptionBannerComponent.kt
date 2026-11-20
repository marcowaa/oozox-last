package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.SubscriptionTier
import com.example.data.model.UserEntity
import com.example.ui.theme.*
import com.example.util.*

@Composable
fun SubscriptionStatusTopBanner(
    currentUser: UserEntity?,
    onOpenSubscriptions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val subDetails = remember(currentUser?.subscriptionTier, currentUser?.subscriptionEndTimestamp, currentUser?.subscriptionStartTimestamp) {
        SubscriptionFeatureGate.evaluateSubscription(currentUser)
    }

    var isExpanded by remember { mutableStateOf(false) }

    val bannerContainerColor = when (subDetails.phase) {
        SubscriptionPhase.TRIAL_ACTIVE -> if (subDetails.trialDaysRemaining <= 5) Color(0xFF78350F).copy(alpha = 0.25f) else Color(0xFF064E3B).copy(alpha = 0.2f)
        SubscriptionPhase.GRACE_WARNING_STAGE_1 -> Color(0xFF7C2D12).copy(alpha = 0.25f)
        SubscriptionPhase.GRACE_WARNING_STAGE_2 -> Color(0xFF881337).copy(alpha = 0.3f)
        SubscriptionPhase.BASE_RESTRICTED -> Color(0xFF450A0A).copy(alpha = 0.35f)
        SubscriptionPhase.PAID_ACTIVE -> GoldDark.copy(alpha = 0.3f)
        SubscriptionPhase.PAID_EXPIRED -> Color(0xFF78350F).copy(alpha = 0.3f)
    }

    val bannerBorderColor = when (subDetails.phase) {
        SubscriptionPhase.TRIAL_ACTIVE -> if (subDetails.trialDaysRemaining <= 5) StatusOrange else StatusGreen
        SubscriptionPhase.GRACE_WARNING_STAGE_1 -> StatusOrange
        SubscriptionPhase.GRACE_WARNING_STAGE_2 -> Color(0xFFE11D48)
        SubscriptionPhase.BASE_RESTRICTED -> StatusRed
        SubscriptionPhase.PAID_ACTIVE -> GoldPrimary
        SubscriptionPhase.PAID_EXPIRED -> StatusOrange
    }

    val bannerIcon = when (subDetails.phase) {
        SubscriptionPhase.TRIAL_ACTIVE -> Icons.Default.Verified
        SubscriptionPhase.GRACE_WARNING_STAGE_1 -> Icons.Default.WarningAmber
        SubscriptionPhase.GRACE_WARNING_STAGE_2 -> Icons.Default.ReportProblem
        SubscriptionPhase.BASE_RESTRICTED -> Icons.Default.Lock
        SubscriptionPhase.PAID_ACTIVE -> Icons.Default.WorkspacePremium
        SubscriptionPhase.PAID_EXPIRED -> Icons.Default.TimerOff
    }

    val iconTint = when (subDetails.phase) {
        SubscriptionPhase.TRIAL_ACTIVE -> if (subDetails.trialDaysRemaining <= 5) StatusOrange else StatusGreen
        SubscriptionPhase.GRACE_WARNING_STAGE_1 -> StatusOrange
        SubscriptionPhase.GRACE_WARNING_STAGE_2 -> Color(0xFFFB7185)
        SubscriptionPhase.BASE_RESTRICTED -> StatusRed
        SubscriptionPhase.PAID_ACTIVE -> GoldPrimary
        SubscriptionPhase.PAID_EXPIRED -> StatusOrange
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("subscription_status_top_banner"),
        shape = RoundedCornerShape(14.dp),
        color = bannerContainerColor,
        border = BorderStroke(1.2.dp, bannerBorderColor.copy(alpha = 0.8f)),
        tonalElevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { isExpanded = !isExpanded }
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(iconTint.copy(alpha = 0.2f), CircleShape)
                            .border(1.dp, iconTint.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = bannerIcon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = subDetails.alertTitle,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = subDetails.alertMessage,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = if (isExpanded) Int.MAX_VALUE else 1
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onOpenSubscriptions,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (subDetails.phase == SubscriptionPhase.PAID_ACTIVE) GoldDark else iconTint
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("btn_banner_upgrade")
                ) {
                    Text(
                        text = if (subDetails.phase == SubscriptionPhase.PAID_ACTIVE) "الباقات" else "ترقية",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (subDetails.phase == SubscriptionPhase.PAID_ACTIVE) GoldLight else Color.Black
                    )
                }
            }

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
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (subDetails.trialDaysRemaining <= 5) StatusOrange else StatusGreen,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Text(
                        text = "${subDetails.trialDaysRemaining}/60 يوم",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (subDetails.trialDaysRemaining <= 5) StatusOrange else StatusGreen
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) {
                    HorizontalDivider(color = bannerBorderColor.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "نظام التدرج الزمني للاشتراكات في OOZOX:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    val stages = listOf(
                        Triple("1. أول 60 يوماً:", "فترة تجريبية مجانية بكامل المزايا", subDetails.phase == SubscriptionPhase.TRIAL_ACTIVE),
                        Triple("2. الأيام 61 - 70:", "المرحلة الأولى: تقييد المحاسب الذكي والمزامنة السحابية", subDetails.phase == SubscriptionPhase.GRACE_WARNING_STAGE_1),
                        Triple("3. الأيام 71 - 80:", "المرحلة الثانية: تقييد تصدير Excel وتدقيق الشكاوى", subDetails.phase == SubscriptionPhase.GRACE_WARNING_STAGE_2),
                        Triple("4. بعد اليوم 80:", "تقييد المزايا المتقدمة حتى الترقية لباقة مدفوعة", subDetails.phase == SubscriptionPhase.BASE_RESTRICTED)
                    )

                    stages.forEach { (title, desc, isCurrent) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Column {
                                Text(
                                    text = title,
                                    fontSize = 11.sp,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                                    color = if (isCurrent) iconTint else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = desc,
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
}

@Composable
fun FeatureGatedDialog(
    decision: FeatureAccessDecision,
    onDismiss: () -> Unit,
    onOpenSubscriptions: () -> Unit
) {
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
            border = BorderStroke(
                1.dp,
                StatusOrange.copy(alpha = 0.5f)
            )
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
                                .background(StatusOrange.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = StatusOrange,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "ميزة مقيدة بالاشتراك",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = decision.feature.titleAr,
                                fontSize = 11.sp,
                                color = StatusOrange
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
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
                        text = decision.restrictionMessage ?: "هذه الميزة تتطلب ترقية حسابك بعد انتهاء الفترة التجريبية المجانية.",
                        fontSize = 12.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 17.sp
                    )

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("الباقة المطلوبة لفتح الميزة:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    "${decision.suggestedTier.label} (${decision.suggestedTier.badgeText})",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
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
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("إغلاق", fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            onDismiss()
                            onOpenSubscriptions()
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.weight(1.8f)
                    ) {
                        Icon(Icons.Default.CreditCard, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("استعراض الباقات", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                    }
                }
            }
        }
    }
}
