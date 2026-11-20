package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.ProjectEntity
import com.example.data.model.ShiftHandoverEntity
import com.example.ui.theme.*
import com.example.util.GoldUtils
import com.example.util.ProjectAccountingProfile
import java.util.UUID

/**
 * Context-Aware Side Drawer specifically tailored for ProjectDetailScreen.
 * Moves secondary management tools (Excel Studio, RBAC & Promotion, PDF Export, Share, Sync, Delete)
 * off the main screen to eliminate visual clutter.
 */
@Composable
fun ProjectDetailSideDrawerContent(
    project: ProjectEntity,
    profile: ProjectAccountingProfile,
    onSelectTab: (Int) -> Unit,
    onAssignFieldTask: () -> Unit = {},
    onOpenSmartExcel: () -> Unit,
    onOpenRbacAndPromotion: () -> Unit,
    onExportPdf: () -> Unit,
    onShareProject: () -> Unit,
    onTriggerSync: () -> Unit,
    onDeleteProject: () -> Unit,
    onCloseDrawer: () -> Unit
) {
    val netBalance = project.totalIncome - project.totalExpenses

    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerContentColor = MaterialTheme.colorScheme.onSurface,
        drawerTonalElevation = 0.dp,
        modifier = Modifier
            .fillMaxHeight()
            .widthIn(min = 310.dp, max = 350.dp)
            .testTag("project_detail_side_drawer")
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
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
                                .background(GoldPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DashboardCustomize,
                                contentDescription = null,
                                tint = GoldPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "أدوات إدارة المشروع",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = profile.sectorNameAr,
                                fontSize = 10.5.sp,
                                color = GoldPrimary,
                                maxLines = 1
                            )
                        }
                    }

                    IconButton(
                        onClick = onCloseDrawer,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق القائمة")
                    }
                }
            }

            item {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                )
            }
                // Quick Financial Summary Card
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        ),
                        border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = project.name,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("صافي الرصيد", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = GoldUtils.formatCurrency(netBalance),
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (netBalance >= 0) StatusGreen else StatusRed
                                )
                            }
                        }
                    }
                }

                // Quick Tab Navigation
                item {
                    Text(
                        text = "أقسام المشروع:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        ProjectDrawerActionItem(
                            icon = Icons.Default.Insights,
                            title = "نظرة عامة والمؤشرات",
                            subtitle = "رأس المال والانحراف المالي",
                            tint = GoldPrimary,
                            onClick = { onSelectTab(0); onCloseDrawer() }
                        )
                        ProjectDrawerActionItem(
                            icon = Icons.Default.AccountBalanceWallet,
                            title = "السجل المالي للمشروع",
                            subtitle = profile.sectorNameAr,
                            tint = Color(0xFF10B981),
                            onClick = { onSelectTab(1); onCloseDrawer() }
                        )
                        ProjectDrawerActionItem(
                            icon = Icons.Default.Checklist,
                            title = "المهام والتوثيق الميداني",
                            subtitle = "متابعة التنفيذ وتوثيق المهام",
                            tint = GoldAccent,
                            onClick = { onSelectTab(2); onCloseDrawer() }
                        )
                        ProjectDrawerActionItem(
                            icon = Icons.Default.Handshake,
                            title = "تسليم الشفتات والعهدة",
                            subtitle = "نقدية، إنستاباي، وجرد البضائع",
                            tint = Color(0xFF3B82F6),
                            onClick = { onSelectTab(3); onCloseDrawer() }
                        )
                    }
                }

                item {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                    )
                    Text(
                        text = "أدوات الإدارة والرقابة:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        ProjectDrawerActionItem(
                            icon = Icons.Default.AssignmentInd,
                            title = "إسناد مهمة مع منشن للموظف",
                            subtitle = "توجيه مهمة ميدانية وتنبيه الفريق",
                            tint = GoldPrimary,
                            onClick = { onCloseDrawer(); onAssignFieldTask() }
                        )
                        ProjectDrawerActionItem(
                            icon = Icons.Default.TableChart,
                            title = "جدول البيانات المالي",
                            subtitle = "تحليلات ومعادلات متقدمة",
                            tint = Color(0xFF10B981),
                            onClick = { onCloseDrawer(); onOpenSmartExcel() }
                        )
                        ProjectDrawerActionItem(
                            icon = Icons.Default.AdminPanelSettings,
                            title = "فريق العمل والترقية لمدير",
                            subtitle = "صلاحيات RBAC وترقية الموظفين",
                            tint = GoldPrimary,
                            onClick = { onCloseDrawer(); onOpenRbacAndPromotion() }
                        )
                        ProjectDrawerActionItem(
                            icon = Icons.Default.PictureAsPdf,
                            title = "تصدير تقرير المشروع PDF",
                            subtitle = "ملخص مالي ومهام معتمد",
                            tint = GoldAccent,
                            onClick = { onCloseDrawer(); onExportPdf() }
                        )
                        ProjectDrawerActionItem(
                            icon = Icons.Default.Share,
                            title = "مشاركة بيانات المشروع",
                            subtitle = "إرسال ملخص ورابط المشروع",
                            tint = GoldPrimary,
                            onClick = { onCloseDrawer(); onShareProject() }
                        )
                        ProjectDrawerActionItem(
                            icon = Icons.Default.Sync,
                            title = "مزامنة قاعدة البيانات Room",
                            subtitle = "تحديث البيانات المحلية والسحابية",
                            tint = Color(0xFF3B82F6),
                            onClick = { onCloseDrawer(); onTriggerSync() }
                        )
                        ProjectDrawerActionItem(
                            icon = Icons.Default.DeleteOutline,
                            title = "حذف المشروع نهائياً",
                            subtitle = "إزالة السجلات والمهام المرتبطة",
                            tint = MaterialTheme.colorScheme.error,
                            onClick = { onCloseDrawer(); onDeleteProject() }
                        )
                    }
                }
            }
        }
    }

@Composable
private fun ProjectDrawerActionItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tint: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
        modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(tint.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(subtitle, fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        }
    }
}

/**
 * Tailored Accounting Entry Dialog using the project's sector-specific revenue/expense accounts.
 */
@Composable
fun ProjectAccountingEntryDialog(
    profile: ProjectAccountingProfile,
    initialTitle: String = "",
    initialIsIncome: Boolean = false,
    onDismiss: () -> Unit,
    onConfirmEntry: (title: String, amount: Double, isIncome: Boolean, accountCategory: String) -> Unit
) {
    var isIncome by remember { mutableStateOf(initialIsIncome) }
    var title by remember { mutableStateOf(initialTitle) }
    var amountStr by remember { mutableStateOf("") }
    val accounts = if (isIncome) profile.revenueAccounts else profile.expenseAccounts
    var selectedAccount by remember(isIncome) {
        mutableStateOf(accounts.firstOrNull() ?: if (isIncome) "إيراد عام" else "مصروف عام")
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f),
            shape = RoundedCornerShape(20.dp),
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
                    Column {
                        Text("تسجيل قيد محاسبي مخصص", fontSize = 15.5.sp, fontWeight = FontWeight.Bold)
                        Text(profile.sectorNameAr, fontSize = 11.sp, color = GoldPrimary)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                // Income vs Expense Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = !isIncome,
                        onClick = { isIncome = false },
                        label = { Text("مصروفات / تكاليف", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.TrendingDown, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = StatusOrange.copy(alpha = 0.2f),
                            selectedLabelColor = StatusOrange
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = isIncome,
                        onClick = { isIncome = true },
                        label = { Text("إيرادات / تحصيلات", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = StatusGreen.copy(alpha = 0.2f),
                            selectedLabelColor = StatusGreen
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                Text("البند المحاسبي:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    accounts.forEach { acc ->
                        FilterChip(
                            selected = selectedAccount == acc,
                            onClick = {
                                selectedAccount = acc
                                if (title.isBlank()) title = acc
                            },
                            label = { Text(acc, fontSize = 11.sp) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("بيان العملية / الوصف", fontSize = 12.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("المبلغ", fontSize = 12.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("accounting_entry_amount_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("إلغاء", fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = {
                            val amount = amountStr.toDoubleOrNull() ?: 0.0
                            if (amount > 0.0) {
                                onConfirmEntry(
                                    title.ifBlank { selectedAccount },
                                    amount,
                                    isIncome,
                                    selectedAccount
                                )
                                onDismiss()
                            }
                        },
                        enabled = (amountStr.toDoubleOrNull() ?: 0.0) > 0.0,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GoldPrimary,
                            contentColor = MaterialTheme.colorScheme.background
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("confirm_accounting_entry_btn")
                    ) {
                        Text("حفظ القيد", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Smart Shift Handover Creation Dialog for Financials (Cash, InstaPay, Vodafone Cash, Bank) + Physical Goods.
 */
@Composable
fun CreateProjectShiftHandoverDialog(
    project: ProjectEntity,
    profile: ProjectAccountingProfile,
    onDismiss: () -> Unit,
    onCreateHandover: (ShiftHandoverEntity) -> Unit
) {
    var cashierName by remember { mutableStateOf("") }
    var cashStr by remember { mutableStateOf("") }
    var instaPayStr by remember { mutableStateOf("") }
    var vodafoneCashStr by remember { mutableStateOf("") }
    var bankStr by remember { mutableStateOf("") }
    var expectedCashStr by remember { mutableStateOf("") }
    var productsAddedStr by remember { mutableStateOf("") }
    var productsSoldStr by remember { mutableStateOf("") }
    var goodsNotes by remember { mutableStateOf("") }

    val cash = cashStr.toDoubleOrNull() ?: 0.0
    val insta = instaPayStr.toDoubleOrNull() ?: 0.0
    val vf = vodafoneCashStr.toDoubleOrNull() ?: 0.0
    val bank = bankStr.toDoubleOrNull() ?: 0.0
    val totalCollected = cash + insta + vf + bank
    val expected = expectedCashStr.toDoubleOrNull() ?: totalCollected
    val diff = totalCollected - expected

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.86f),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.35f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("تسليم شفت وعهدة", fontSize = 15.5.sp, fontWeight = FontWeight.Bold)
                        Text("توثيق الماليات والبضائع لمشروع ${project.name}", fontSize = 11.sp, color = GoldPrimary)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        OutlinedTextField(
                            value = cashierName,
                            onValueChange = { cashierName = it },
                            label = { Text("اسم الموظف المسلّم للشفت", fontSize = 12.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        Text("١. التحصيلات المالية للشفت:", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = GoldPrimary)
                    }

                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = cashStr,
                                onValueChange = { cashStr = it },
                                label = { Text("النقدية (كاش)", fontSize = 11.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = instaPayStr,
                                onValueChange = { instaPayStr = it },
                                label = { Text("إنستاباي", fontSize = 11.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = vodafoneCashStr,
                                onValueChange = { vodafoneCashStr = it },
                                label = { Text("فودافون كاش", fontSize = 11.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = bankStr,
                                onValueChange = { bankStr = it },
                                label = { Text("فيزا / بنكي", fontSize = 11.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = expectedCashStr,
                            onValueChange = { expectedCashStr = it },
                            label = { Text("الإجمالي المتوقع بالدفاتر (للمطابقة التلقائية)", fontSize = 11.5.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        Surface(
                            color = if (diff < 0) StatusRed.copy(alpha = 0.1f) else StatusGreen.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("الإجمالي الفعلي: ${GoldUtils.formatCurrency(totalCollected)}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    text = when {
                                        diff < 0 -> "عجز: ${GoldUtils.formatCurrency(-diff)}"
                                        diff > 0 -> "فائض: ${GoldUtils.formatCurrency(diff)}"
                                        else -> "مطابق 100% ✓"
                                    },
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (diff < 0) StatusRed else StatusGreen
                                )
                            }
                        }
                    }

                    if (profile.tracksPhysicalGoods) {
                        item {
                            Text("٢. عهدة البضائع والمخزون:", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = GoldPrimary)
                        }
                        item {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = productsAddedStr,
                                    onValueChange = { productsAddedStr = it },
                                    label = { Text("بضاعة مضافة (عدد)", fontSize = 11.sp) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = productsSoldStr,
                                    onValueChange = { productsSoldStr = it },
                                    label = { Text("بضاعة منصرفة/مباعة", fontSize = 11.sp) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = goodsNotes,
                            onValueChange = { goodsNotes = it },
                            label = { Text("ملاحظات الجرد والعهدة (${profile.shiftHandoverChecklist.firstOrNull() ?: ""})", fontSize = 11.5.sp) },
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

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
                            val now = System.currentTimeMillis()
                            val handover = ShiftHandoverEntity(
                                id = UUID.randomUUID().toString(),
                                cashierId = "staff_$now",
                                cashierName = cashierName.ifBlank { "موظف الوردية" },
                                loginTime = now - (8 * 3600 * 1000L),
                                logoutTime = now,
                                totalInvoicesCount = (productsSoldStr.toIntOrNull() ?: 0).coerceAtLeast(1),
                                totalRevenueEGP = totalCollected,
                                totalPaidEGP = 0.0,
                                cashCollected = cash,
                                instaPayCollected = insta,
                                vodafoneCashCollected = vf,
                                bankCollected = bank,
                                netTreasuryCash = cash,
                                totalIncomingTransactions = totalCollected,
                                totalOutgoingTransactions = 0.0,
                                expectedCash = expected,
                                actualCash = totalCollected,
                                deficitAmount = if (diff < 0) -diff else 0.0,
                                surplusAmount = if (diff > 0) diff else 0.0,
                                productsAddedCount = productsAddedStr.toIntOrNull() ?: 0,
                                productsSoldCount = productsSoldStr.toIntOrNull() ?: 0,
                                notes = buildString {
                                    append("[مشروع: ${project.name}]")
                                    if (goodsNotes.isNotBlank()) append(" $goodsNotes")
                                },
                                merchandiseStatus = if (profile.tracksPhysicalGoods) "جرد بضائع ومخزون الوردية" else "عهدة تشغيلية"
                            )
                            onCreateHandover(handover)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GoldPrimary,
                            contentColor = MaterialTheme.colorScheme.background
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("submit_shift_handover_btn")
                    ) {
                        Text("تسليم الشفت", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
