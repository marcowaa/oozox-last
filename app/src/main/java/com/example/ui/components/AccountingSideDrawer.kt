package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.HiddenProfileEntity
import com.example.ui.theme.*
import com.example.util.AccountingI18n
import com.example.util.AppLanguage
import com.example.util.LocalAppLanguage

@Composable
fun AccountingSideDrawerContent(
    activeSection: Int,
    activeProfile: HiddenProfileEntity?,
    recordsCount: Int,
    groupsCount: Int,
    hiddenProfilesCount: Int,
    alertsCount: Int,
    onSelectSection: (Int) -> Unit,
    onOpenVaultAuth: () -> Unit,
    onLockToMainProfile: () -> Unit,
    onAddRecord: () -> Unit,
    onOpenVoiceAssistant: () -> Unit,
    onOpenComplaints: () -> Unit,
    onOpenRetroComplaint: () -> Unit,
    onExportUnifiedExcel: () -> Unit,
    onExportPdf: () -> Unit,
    onLanguageChange: (AppLanguage) -> Unit,
    onCloseDrawer: () -> Unit
) {
    val currentLang = LocalAppLanguage.current
    val str: (String) -> String = { key -> AccountingI18n.get(key, currentLang) }
    val primaryGold = adaptiveGoldPrimary
    val onPrimary = adaptiveOnPrimary
    val statusGreen = adaptiveStatusGreen
    val statusRed = adaptiveStatusRed
    val statusOrange = adaptiveStatusOrange

    ModalDrawerSheet(
        modifier = Modifier
            .widthIn(min = 296.dp, max = 340.dp)
            .fillMaxHeight()
            .testTag("accounting_side_drawer_sheet"),
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerContentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = primaryGold.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, primaryGold.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
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
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(
                                                Brush.linearGradient(listOf(primaryGold, RealGoldAmber))
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AccountBalance,
                                            contentDescription = str("hub_title"),
                                            tint = onPrimary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = str("hub_title"),
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 15.5.sp,
                                            color = primaryGold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = activeProfile?.name ?: str("drawer_subtitle"),
                                            fontSize = 11.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = onCloseDrawer,
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("accounting_drawer_close_top_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = str("close_drawer"),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Active Vault Status / Switcher
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (activeProfile != null) {
                                    Surface(
                                        color = adaptiveStatusGreen.copy(alpha = 0.16f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = str("encrypted_badge"),
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = adaptiveStatusGreen,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.weight(1f))
                                    FilledTonalButton(
                                        onClick = {
                                            onCloseDrawer()
                                            onLockToMainProfile()
                                        },
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = adaptiveStatusRed.copy(alpha = 0.16f),
                                            contentColor = adaptiveStatusRed
                                        ),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.heightIn(min = 38.dp)
                                    ) {
                                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(str("lock_vault"), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    FilledTonalButton(
                                        onClick = {
                                            onCloseDrawer()
                                            onOpenVaultAuth()
                                        },
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = primaryGold.copy(alpha = 0.18f),
                                            contentColor = primaryGold
                                        ),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 42.dp)
                                    ) {
                                        Icon(Icons.Default.VpnKey, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(str("open_vault"), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                // Modules Section
                item {
                    Text(
                        text = str("departments_nav"),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }

                val modules = listOf(
                    Triple(-1, str("sec_dashboard"), Icons.Default.Dashboard),
                    Triple(1, str("sec_ledger"), Icons.Default.ReceiptLong),
                    Triple(2, str("sec_groups"), Icons.Default.Business),
                    Triple(3, str("sec_staff"), Icons.Default.People),
                    Triple(0, str("sec_vaults"), Icons.Default.Security),
                    Triple(4, str("sec_backup"), Icons.Default.CloudSync)
                )

                items(modules.size) { index ->
                    val (secId, label, icon) = modules[index]
                    val badgeCount = when (secId) {
                        1 -> recordsCount
                        2 -> groupsCount
                        0 -> hiddenProfilesCount
                        -1 -> alertsCount
                        else -> 0
                    }
                    AccountingDrawerItemRow(
                        title = label,
                        icon = icon,
                        selected = activeSection == secId,
                        badgeCount = badgeCount,
                        badgeColor = if (secId == -1 && alertsCount > 0) adaptiveStatusRed else primaryGold,
                        testTag = "accounting_drawer_sec_$secId",
                        onClick = {
                            onCloseDrawer()
                            onSelectSection(secId)
                        }
                    )
                }

                // Quick Commands Section
                item {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    Text(
                        text = str("quick_actions"),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }

                val tools = listOf(
                    Triple(str("cmd_new_record"), Icons.Default.PostAdd, primaryGold to onAddRecord),
                    Triple(str("cmd_voice"), Icons.Default.Mic, primaryGold to onOpenVoiceAssistant),
                    Triple(str("cmd_excel"), Icons.Default.TableChart, statusGreen to onExportUnifiedExcel),
                    Triple(str("cmd_complaints"), Icons.Default.ReportProblem, statusRed to onOpenComplaints),
                    Triple(str("cmd_retro"), Icons.Default.History, statusOrange to onOpenRetroComplaint),
                    Triple(str("cmd_pdf"), Icons.Default.PictureAsPdf, primaryGold to onExportPdf)
                )

                items(tools.size) { index ->
                    val (label, icon, pair) = tools[index]
                    val (tint, action) = pair
                    AccountingDrawerToolRow(
                        title = label,
                        icon = icon,
                        tint = tint,
                        onClick = {
                            onCloseDrawer()
                            action()
                        }
                    )
                }

                // Language Toggle Pills
                item {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = str("language"),
                                tint = primaryGold,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = str("language"),
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(
                                AppLanguage.ARABIC to "العربية",
                                AppLanguage.ENGLISH to "EN"
                            ).forEach { (lang, codeLabel) ->
                                val isSel = currentLang == lang
                                Surface(
                                    onClick = { onLanguageChange(lang) },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSel) primaryGold else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                    modifier = Modifier.heightIn(min = 38.dp)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = codeLabel,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSel) onPrimary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Close Drawer Action Bar
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                OutlinedButton(
                    onClick = onCloseDrawer,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("accounting_drawer_close_bottom_btn")
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = str("close_drawer"),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountingDrawerItemRow(
    title: String,
    icon: ImageVector,
    selected: Boolean,
    badgeCount: Int,
    badgeColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    val primaryGold = adaptiveGoldPrimary
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) primaryGold.copy(alpha = 0.14f) else Color.Transparent,
        border = if (selected) BorderStroke(1.dp, primaryGold.copy(alpha = 0.4f)) else null,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = if (selected) primaryGold else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = title,
                    fontSize = 13.5.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = if (selected) primaryGold else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (badgeCount > 0) {
                CircularCounterBadge(
                    count = badgeCount,
                    containerColor = badgeColor,
                    contentColor = if (badgeColor == adaptiveStatusRed) Color.White else adaptiveOnPrimary
                )
            }
        }
    }
}

@Composable
private fun AccountingDrawerToolRow(
    title: String,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
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
                        .size(30.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(tint.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
                }
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(13.dp)
            )
        }
    }
}
