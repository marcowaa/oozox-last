package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.HiddenSubAccountEntity
import com.example.data.model.UserEntity
import com.example.ui.theme.*
import com.example.util.tr
import com.example.util.trFmt

/**
 * Dedicated, brand-aligned Side Navigation Drawer specifically for the Business Management Section.
 * Holds the Premium Trial banner, Invoice Verification tools, Profession settings,
 * cashier controls, and secondary business actions, freeing the main screen to be spacious, clean, and fast.
 */
@Composable
fun BusinessSideDrawerContent(
    currentUser: UserEntity?,
    activeProfession: String,
    activeSubAccount: HiddenSubAccountEntity?,
    trialDaysRemaining: Int,
    pendingShiftsCount: Int = 0,
    complaintsCount: Int = 0,
    onOpenSubscriptions: () -> Unit,
    onOpenInvoiceVerification: () -> Unit,
    onOpenProfessionPicker: () -> Unit,
    onOpenCashierLogin: () -> Unit,
    onOpenShiftHandover: () -> Unit,
    onOpenPaymentMethods: () -> Unit,
    onOpenEndOfDayReport: () -> Unit,
    onOpenCompanyAccounting: () -> Unit,
    onOpenRetroactiveLedger: () -> Unit = {},
    onOpenAccountingExcel: () -> Unit = {},
    onCloseDrawer: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerContentColor = MaterialTheme.colorScheme.onSurface,
        drawerTonalElevation = 0.dp,
        modifier = Modifier
            .fillMaxHeight()
            .widthIn(min = 320.dp, max = 360.dp)
            .testTag("business_side_drawer")
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Header Bar: Title, Icon & Close Button
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(OozoxTeal.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.BusinessCenter,
                                contentDescription = null,
                                tint = OozoxTeal,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = tr("bdrawer.title"),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = tr("bdrawer.subtitle"),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    IconButton(
                        onClick = onCloseDrawer,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = tr("common.close"),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            item {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                )
            }

            // Active Merchant / Cashier Profile Summary
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(GoldPrimary.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Storefront, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(20.dp))
                            }
                            Column {
                                Text(
                                    text = currentUser?.name ?: tr("bdrawer.verified_merchant"),
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (activeSubAccount != null) {
                                        trFmt("bdrawer.cashier_fmt", "name" to activeSubAccount.displayName)
                                    } else {
                                        trFmt("bdrawer.profession_fmt", "name" to activeProfession)
                                    },
                                    fontSize = 11.sp,
                                    color = if (activeSubAccount != null) OozoxTeal else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                onCloseDrawer()
                                onOpenProfessionPicker()
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(GoldPrimary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = tr("bdrawer.edit_profession"), tint = GoldPrimary, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            // 1. Premium Free Trial Banner (Relocated into side drawer)
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = GoldPrimary.copy(alpha = 0.12f)
                    ),
                    border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
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
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(GoldPrimary.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.CardMembership, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(18.dp))
                                }
                                Column {
                                    Text(
                                        text = tr("bdrawer.trial_title"),
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = trFmt("bdrawer.trial_days_fmt", "days" to trialDaysRemaining),
                                        fontSize = 10.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Text(
                            text = tr("bdrawer.trial_desc"),
                            fontSize = 10.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 15.sp
                        )

                        Button(
                            onClick = {
                                onCloseDrawer()
                                onOpenSubscriptions()
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Text("${tr("bdrawer.view_vip_plans")} ↗", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 2. Invoice Verification Quick Action Banner (Relocated into side drawer)
            item {
                Surface(
                    onClick = {
                        onCloseDrawer()
                        onOpenInvoiceVerification()
                    },
                    shape = RoundedCornerShape(14.dp),
                    color = OozoxTeal.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, OozoxTeal.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
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
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(OozoxTeal.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = OozoxTeal, modifier = Modifier.size(20.dp))
                            }
                            Column {
                                Text(
                                    text = "${tr("bdrawer.verify_banner_title")} 🛡️",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = tr("bdrawer.verify_banner_sub"),
                                    fontSize = 10.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, tint = OozoxTeal, modifier = Modifier.size(14.dp))
                    }
                }
            }

            // Section: Quick Business Tools
            item {
                Text(
                    text = tr("bdrawer.quick_section"),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                )
            }

            item {
                DrawerActionTile(
                    icon = Icons.Default.TableChart,
                    iconTint = Color(0xFF10B981),
                    title = tr("bdrawer.excel_title"),
                    subtitle = tr("bdrawer.excel_sub"),
                    badge = "Excel",
                    onClick = {
                        onCloseDrawer()
                        onOpenAccountingExcel()
                    }
                )
            }

            item {
                DrawerActionTile(
                    icon = Icons.Default.AccountBalanceWallet,
                    iconTint = GoldPrimary,
                    title = tr("bdrawer.retro_title"),
                    subtitle = tr("bdrawer.retro_sub"),
                    badge = if (pendingShiftsCount > 0) {
                        trFmt("bdrawer.pending_shifts_fmt", "count" to pendingShiftsCount)
                    } else if (complaintsCount > 0) {
                        trFmt("bdrawer.complaints_fmt", "count" to complaintsCount)
                    } else {
                        null
                    },
                    onClick = {
                        onCloseDrawer()
                        onOpenRetroactiveLedger()
                    }
                )
            }

            item {
                DrawerActionTile(
                    icon = Icons.Default.CorporateFare,
                    iconTint = GoldPrimary,
                    title = tr("bdrawer.company_hub_title"),
                    subtitle = tr("bdrawer.company_hub_sub"),
                    badge = "PRO",
                    onClick = {
                        onCloseDrawer()
                        onOpenCompanyAccounting()
                    }
                )
            }

            item {
                DrawerActionTile(
                    icon = Icons.Default.Assessment,
                    iconTint = Color(0xFF8B5CF6),
                    title = tr("bdrawer.eod_title"),
                    subtitle = tr("bdrawer.eod_sub"),
                    badge = tr("bdrawer.eod_badge"),
                    onClick = {
                        onCloseDrawer()
                        onOpenEndOfDayReport()
                    }
                )
            }

            item {
                DrawerActionTile(
                    icon = Icons.Default.Payments,
                    iconTint = Color(0xFF14B8A6),
                    title = tr("bdrawer.pay_title"),
                    subtitle = tr("bdrawer.pay_sub"),
                    badge = null,
                    onClick = {
                        onCloseDrawer()
                        onOpenPaymentMethods()
                    }
                )
            }

            item {
                DrawerActionTile(
                    icon = Icons.Default.LockClock,
                    iconTint = Color(0xFFEF4444),
                    title = tr("bdrawer.shift_title"),
                    subtitle = tr("bdrawer.shift_sub"),
                    badge = if (activeSubAccount != null) tr("bdrawer.active_shift") else null,
                    onClick = {
                        onCloseDrawer()
                        onOpenShiftHandover()
                    }
                )
            }

            item {
                DrawerActionTile(
                    icon = Icons.Default.Badge,
                    iconTint = Color(0xFF0EA5E9),
                    title = tr("bdrawer.cashier_login_title"),
                    subtitle = tr("bdrawer.cashier_login_sub"),
                    badge = null,
                    onClick = {
                        onCloseDrawer()
                        onOpenCashierLogin()
                    }
                )
            }

        }
    }
}

@Composable
private fun DrawerActionTile(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    badge: String?,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(iconTint.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(17.dp))
                }
                Column {
                    Text(
                        text = title,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (badge != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = iconTint.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = badge,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = iconTint,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
