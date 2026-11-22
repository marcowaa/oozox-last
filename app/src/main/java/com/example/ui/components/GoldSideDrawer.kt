package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserEntity
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.adaptiveGoldPrimary
import com.example.util.AppLanguage
import com.example.util.LocalAppLanguage
import com.example.util.Translations
import com.example.util.tr

enum class GoldDrawerSection(
    val titleAr: String,
    val subtitleAr: String,
    val icon: ImageVector
) {
    MARKET(
        titleAr = "الأسعار والحاسبة",
        subtitleAr = "أسعار الذهب والفضة وحساب المصنعية",
        icon = Icons.Default.Diamond
    ),
    PORTFOLIO(
        titleAr = "سجل الحيازة والفواتير",
        subtitleAr = "متابعة المشتريات والأوزان الصافية",
        icon = Icons.Default.ReceiptLong
    ),
    ALERTS(
        titleAr = "تنبيهات الأسعار",
        subtitleAr = "إشعارات فورية عند الوصول للسعر المستهدف",
        icon = Icons.Default.NotificationsActive
    ),
    ZAKAT(
        titleAr = "حاسبة الزكاة",
        subtitleAr = "حساب النصاب الشرعي ومقدار الزكاة",
        icon = Icons.Default.AccountBalance
    ),
    ADVISOR(
        titleAr = "المستشار المالي",
        subtitleAr = "تحليل توقيت الشراء والبيع والتحوط",
        icon = Icons.Default.Insights
    ),
    QR_CENTER(
        titleAr = "مركز فحص وبطاقات QR",
        subtitleAr = "مسح وتوليد رموز المشتريات",
        icon = Icons.Default.QrCode2
    );

    fun localizedTitle(lang: AppLanguage): String = when (this) {
        MARKET -> Translations.get("gdrawer.sec_market", lang)
        PORTFOLIO -> Translations.get("gdrawer.sec_portfolio", lang)
        ALERTS -> Translations.get("gdrawer.sec_alerts", lang)
        ZAKAT -> Translations.get("gdrawer.sec_zakat", lang)
        ADVISOR -> Translations.get("gdrawer.sec_advisor", lang)
        QR_CENTER -> Translations.get("gdrawer.sec_qr", lang)
    }

    fun localizedSubtitle(lang: AppLanguage): String = when (this) {
        MARKET -> Translations.get("gdrawer.sec_market_sub", lang)
        PORTFOLIO -> Translations.get("gdrawer.sec_portfolio_sub", lang)
        ALERTS -> Translations.get("gdrawer.sec_alerts_sub", lang)
        ZAKAT -> Translations.get("gdrawer.sec_zakat_sub", lang)
        ADVISOR -> Translations.get("gdrawer.sec_advisor_sub", lang)
        QR_CENTER -> Translations.get("gdrawer.sec_qr_sub", lang)
    }
}

@Composable
fun GoldSideDrawerContent(
    currentUser: UserEntity?,
    selectedSection: GoldDrawerSection,
    purchasesCount: Int,
    activeAlertsCount: Int,
    onSelectSection: (GoldDrawerSection) -> Unit,
    onOpenMerchantStore: () -> Unit,
    onAddPurchase: () -> Unit,
    onOpenQrScanner: () -> Unit,
    onOpenQrGenerator: () -> Unit,
    onCloseDrawer: () -> Unit
) {
    val lang = LocalAppLanguage.current

    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerContentColor = MaterialTheme.colorScheme.onSurface,
        drawerTonalElevation = 0.dp,
        modifier = Modifier
            .fillMaxHeight()
            .widthIn(min = 300.dp, max = 348.dp)
            .testTag("gold_side_drawer")
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Drawer Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
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
                                .background(adaptiveGoldPrimary.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Diamond,
                                contentDescription = null,
                                tint = adaptiveGoldPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = tr("gdrawer.title"),
                                fontSize = 15.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = currentUser?.name ?: tr("gdrawer.fallback_sub"),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
            }

            // Primary Gold Sections
            items(GoldDrawerSection.entries.size) { index ->
                val section = GoldDrawerSection.entries[index]
                val isSelected = selectedSection == section
                val badge = when (section) {
                    GoldDrawerSection.PORTFOLIO -> if (purchasesCount > 0) "$purchasesCount" else null
                    GoldDrawerSection.ALERTS -> if (activeAlertsCount > 0) "$activeAlertsCount" else null
                    else -> null
                }

                Surface(
                    onClick = {
                        onSelectSection(section)
                        onCloseDrawer()
                    },
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSelected) {
                        adaptiveGoldPrimary.copy(alpha = 0.14f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f)
                    },
                    border = BorderStroke(
                        width = if (isSelected) 1.2.dp else 0.8.dp,
                        color = if (isSelected) {
                            adaptiveGoldPrimary.copy(alpha = 0.7f)
                        } else {
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("gold_drawer_item_${section.name.lowercase()}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
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
                                    .background(
                                        if (isSelected) adaptiveGoldPrimary.copy(alpha = 0.2f)
                                        else MaterialTheme.colorScheme.surface
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = section.icon,
                                    contentDescription = null,
                                    tint = if (isSelected) adaptiveGoldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = section.localizedTitle(lang),
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                    color = if (isSelected) adaptiveGoldPrimary else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = section.localizedSubtitle(lang),
                                    fontSize = 10.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (badge != null) {
                            Surface(
                                shape = CircleShape,
                                color = adaptiveGoldPrimary.copy(alpha = 0.16f)
                            ) {
                                Text(
                                    text = badge,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = adaptiveGoldPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        } else if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = adaptiveGoldPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            item {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            // Quick Actions inside Drawer
            item {
                Text(
                    text = tr("gdrawer.shortcuts"),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            item {
                GoldDrawerQuickRow(
                    icon = Icons.Default.Storefront,
                    title = tr("gdrawer.merchant_store"),
                    onClick = {
                        onCloseDrawer()
                        onOpenMerchantStore()
                    }
                )
            }

            item {
                GoldDrawerQuickRow(
                    icon = Icons.Default.AddCircleOutline,
                    title = tr("gdrawer.add_purchase"),
                    onClick = {
                        onCloseDrawer()
                        onAddPurchase()
                    }
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            onCloseDrawer()
                            onOpenQrScanner()
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(tr("gdrawer.scan_qr"), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    OutlinedButton(
                        onClick = {
                            onCloseDrawer()
                            onOpenQrGenerator()
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(Icons.Default.QrCode2, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(tr("gdrawer.gen_qr"), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun GoldDrawerQuickRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(icon, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(18.dp))
                Text(
                    text = title,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(15.dp)
            )
        }
    }
}
