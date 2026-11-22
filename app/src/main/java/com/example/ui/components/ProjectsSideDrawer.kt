package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.example.data.model.ProjectEntity
import com.example.data.model.ProjectType
import com.example.ui.theme.*
import com.example.util.GoldUtils
import com.example.util.LocalAppLanguage
import com.example.util.Translations
import com.example.util.tr
import com.example.util.trFmt

/**
 * Filter and view options for the custom Projects Side Drawer.
 */
enum class ProjectDrawerCategory {
    ALL,
    ACTIVE,
    UNDER_BUDGET,
    OVER_BUDGET,
    FOUNDING
}

/**
 * Dedicated, brand-aligned Side Navigation Drawer specifically for the Projects Section.
 * Contains all project settings, filters, KPI cards, analytics switches, and biometric controls,
 * freeing the main screen to be spacious, clean, and full-screen for the client.
 */
@Composable
fun ProjectsSideDrawerContent(
    projects: List<ProjectEntity>,
    selectedStatusFilter: String?,
    selectedTypeFilter: String?,
    availableTypes: List<String>,
    availableStatuses: List<String>,
    onSelectStatusFilter: (String?) -> Unit,
    onSelectTypeFilter: (String?) -> Unit,
    showAnalytics: Boolean,
    onToggleAnalytics: () -> Unit,
    isBiometricLocked: Boolean,
    onToggleBiometricLock: () -> Unit,
    isNetworkOnline: Boolean,
    onTriggerSync: () -> Unit,
    show24hBackupPrompt: Boolean,
    isBackingUp: Boolean,
    onPerformBackup: () -> Unit,
    onCreateProjectClick: () -> Unit,
    onCloseDrawer: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val lang = LocalAppLanguage.current

    val totalCapital = remember(projects) { projects.sumOf { it.estimatedCapital } }
    val totalExpenses = remember(projects) { projects.sumOf { it.totalExpenses } }
    val overallBurnRate = remember(totalCapital, totalExpenses) {
        if (totalCapital > 0) ((totalExpenses / totalCapital) * 100).toInt().coerceIn(0, 100) else 0
    }

    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerContentColor = MaterialTheme.colorScheme.onSurface,
        drawerTonalElevation = 0.dp,
        modifier = Modifier
            .fillMaxHeight()
            .widthIn(min = 320.dp, max = 360.dp)
            .testTag("projects_side_drawer")
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
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
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Widgets,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = tr("pdrawer.title"),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = tr("pdrawer.subtitle"),
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
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )
            }
                // Section 1: Quick Action New Project
                item {
                    Button(
                        onClick = {
                            onCloseDrawer()
                            onCreateProjectClick()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(tr("pdrawer.create_btn"), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                // Section 2: Financial KPIs Summary
                item {
                    Text(
                        text = tr("pdrawer.kpi_title"),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(tr("pdrawer.total_capital"), fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    if (isBiometricLocked) "•••••• 🔒" else GoldUtils.formatCurrency(totalCapital),
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(tr("pdrawer.actual_expenses"), fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    if (isBiometricLocked) "•••••• 🔒" else GoldUtils.formatCurrency(totalExpenses),
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StatusOrange
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(tr("pdrawer.burn_rate"), fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    "$overallBurnRate%",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (overallBurnRate > 80) adaptiveStatusRed else adaptiveStatusGreen
                                )
                            }
                        }
                    }
                }

                // Section 3: View Toggles (Analytics Charts)
                item {
                    Text(
                        text = tr("pdrawer.display_tools"),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clickable { onToggleAnalytics() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Analytics,
                                    contentDescription = null,
                                    tint = GoldPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text = tr("pdrawer.analytics_title"),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (showAnalytics) tr("pdrawer.analytics_shown") else tr("pdrawer.analytics_hidden"),
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = showAnalytics,
                                onCheckedChange = { onToggleAnalytics() },
                                colors = SwitchDefaults.colors(checkedThumbColor = GoldPrimary)
                            )
                        }
                    }
                }

                // Section 4: Security & Biometric Lock
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isBiometricLocked) Color(0xFFEF4444).copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = BorderStroke(1.dp, if (isBiometricLocked) Color(0xFFEF4444).copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clickable { onToggleBiometricLock() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = if (isBiometricLocked) Icons.Default.Fingerprint else Icons.Default.LockOpen,
                                    contentDescription = null,
                                    tint = if (isBiometricLocked) Color(0xFFEF4444) else Color(0xFF10B981),
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text = tr("pdrawer.biometric_title"),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (isBiometricLocked) tr("pdrawer.biometric_locked") else tr("pdrawer.biometric_open"),
                                        fontSize = 10.sp,
                                        color = if (isBiometricLocked) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = isBiometricLocked,
                                onCheckedChange = { onToggleBiometricLock() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFFEF4444),
                                    checkedTrackColor = Color(0xFFEF4444).copy(alpha = 0.3f)
                                )
                            )
                        }
                    }
                }

                // Section 5: Filter by Status
                item {
                    Text(
                        text = tr("pdrawer.filter_status"),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        availableStatuses.forEach { statusText ->
                            val isAllStatus = statusText == "الكل" || statusText.equals("ALL", ignoreCase = true)
                            val isSelected = (selectedStatusFilter == null && isAllStatus) || selectedStatusFilter == statusText
                            val localizedStatus = when (statusText) {
                                "الكل", "ALL" -> Translations.get("pdrawer.status_all", lang)
                                "نشط", "ACTIVE" -> Translations.get("pdrawer.status_active", lang)
                                "تحت الميزانية", "UNDER_BUDGET" -> Translations.get("pdrawer.status_under_budget", lang)
                                "تجاوز الميزانية", "OVER_BUDGET" -> Translations.get("pdrawer.status_over_budget", lang)
                                "قيد التأسيس", "FOUNDING" -> Translations.get("pdrawer.status_founding", lang)
                                else -> statusText
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent,
                                border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onSelectStatusFilter(if (isAllStatus || isSelected) null else statusText)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = localizedStatus,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Section 6: Filter by Department / Type
                if (availableTypes.isNotEmpty()) {
                    item {
                        Text(
                            text = tr("pdrawer.filter_type"),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            val isAllSelected = selectedTypeFilter == null
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isAllSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent,
                                border = if (isAllSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onSelectTypeFilter(null)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = trFmt("pdrawer.all_depts_fmt", "count" to projects.size),
                                        fontSize = 12.sp,
                                        fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isAllSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (isAllSelected) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            availableTypes.forEach { type ->
                                val isSelected = selectedTypeFilter == type
                                val localizedType = ProjectType.entries.find { it.name.equals(type, ignoreCase = true) }?.localizedLabel(lang) ?: type
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent,
                                    border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onSelectTypeFilter(if (isSelected) null else type)
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = localizedType,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                        if (isSelected) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Section 7: Offline Sync & Room Backup Actions
                item {
                    Text(
                        text = tr("pdrawer.sync_backup"),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isNetworkOnline) tr("pdrawer.sync_online") else tr("pdrawer.sync_offline"),
                                    fontSize = 11.sp,
                                    color = if (isNetworkOnline) MaterialTheme.colorScheme.primary else Color(0xFFD97706),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(
                                    onClick = onTriggerSync,
                                    modifier = Modifier.heightIn(min = 48.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(tr("pdrawer.sync_now_arrow"), fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                }
                            }

                            Button(
                                onClick = onPerformBackup,
                                enabled = !isBackingUp,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                            ) {
                                if (isBackingUp) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = Color.White)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(tr("pdrawer.backing_up"), fontSize = 11.sp, color = Color.White)
                                } else {
                                    Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(tr("pdrawer.upload_backup"), fontSize = 11.5.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }

                    }
                }
            }
        }
    }
}
