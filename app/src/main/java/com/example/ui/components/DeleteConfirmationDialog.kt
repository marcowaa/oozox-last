package com.example.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.ChatEntity
import com.example.data.model.InvoiceEntity
import com.example.data.model.ProjectEntity
import com.example.data.model.TransactionEntity
import com.example.ui.screens.UnifiedInvoiceItem
import com.example.ui.theme.*
import com.example.util.GoldUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Type of record being targeted for deletion.
 */
enum class DeleteRecordType(val title: String, val defaultIcon: ImageVector) {
    PROJECT("مشروع", Icons.Default.FolderDelete),
    CHAT("محادثة وسجلات التراسل", Icons.Default.DeleteForever),
    INVOICE("فاتورة مالية / تسوية", Icons.Default.ReceiptLong),
    TRANSACTION("صفقة ذهب / معاملة", Icons.Default.MonetizationOn),
    PROJECT_TASK("مهمة مشروع", Icons.Default.AssignmentLate),
    GENERIC("عنصر وسجل", Icons.Default.DeleteOutline)
}

/**
 * Key-Value detail item for displaying metadata in the confirmation dialog.
 */
data class RecordDetailItem(
    val label: String,
    val value: String,
    val highlightColor: Color? = null
)

/**
 * Comprehensive, reusable Delete Confirmation Dialog component.
 * Prevents accidental data loss with rich contextual details, danger warnings,
 * haptic feedback, and clear destructive action cues.
 */
@Composable
fun RecordDeleteConfirmationDialog(
    recordType: DeleteRecordType,
    title: String,
    subtitle: String,
    itemName: String,
    details: List<RecordDetailItem> = emptyList(),
    warningMessage: String = "هذا الإجراء نهائي ولا يمكن التراجع عنه. سيتم حذف هذا السجل وبياناته المرتبطة من قاعدة البيانات.",
    confirmButtonText: String = "تأكيد الحذف نهائياً",
    cancelButtonText: String = "إلغاء / تراجع",
    isDeleting: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val context = LocalContext.current

    fun triggerDestructiveHaptic() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.vibrate(70L)
            }
        } catch (_: Exception) {}
    }

    Dialog(
        onDismissRequest = {
            if (!isDeleting) onDismiss()
        },
        properties = DialogProperties(
            dismissOnBackPress = !isDeleting,
            dismissOnClickOutside = !isDeleting,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .testTag("delete_confirmation_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            shadowElevation = 16.dp,
            border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.55f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header Danger Icon
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = recordType.defaultIcon,
                            contentDescription = "تحذير الحذف",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Title & Subtitle
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                // Item Name Card Highlight
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "السجل المستهدف:",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = itemName,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Detailed Key-Value Rows
                        if (details.isNotEmpty()) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                thickness = 0.8.dp
                            )
                            details.forEach { detail ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = detail.label,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = detail.value,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = detail.highlightColor ?: MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                // Warning Notice Banner
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f),
                    border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.WarningAmber,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = warningMessage,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.error,
                            lineHeight = 16.sp
                        )
                    }
                }

                // Action Buttons (Cancel & Confirm Delete)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        enabled = !isDeleting,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("cancel_delete_button")
                    ) {
                        Text(
                            text = cancelButtonText,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Button(
                        onClick = {
                            triggerDestructiveHaptic()
                            onConfirm()
                        },
                        enabled = !isDeleting,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        modifier = Modifier
                            .weight(1.3f)
                            .height(44.dp)
                            .testTag("confirm_delete_button")
                    ) {
                        if (isDeleting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.onError,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onError,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = confirmButtonText,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Dedicated Confirmation Dialog for Deleting a Project.
 */
@Composable
fun DeleteProjectConfirmationDialog(
    project: ProjectEntity,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    var isDeleting by remember { mutableStateOf(false) }

    RecordDeleteConfirmationDialog(
        recordType = DeleteRecordType.PROJECT,
        title = "تأكيد حذف المشروع ️",
        subtitle = "أنت على وشك حذف هذا المشروع وكافة مهامه وسجلاته",
        itemName = project.name,
        details = listOf(
            RecordDetailItem("نوع المشروع:", project.type),
            RecordDetailItem("رأس المال التقديري:", GoldUtils.formatCurrency(project.estimatedCapital), highlightColor = GoldPrimary),
            RecordDetailItem("الموقع / العنوان:", if (project.location.isNotBlank()) project.location else "غير محدد"),
            RecordDetailItem("الشريك الإداري:", project.coManagerName ?: "لا يوجد")
        ),
        warningMessage = "سيؤدي الحذف إلى إزالة بيانات المشروع والمهام وحسابات التكلفة المرتبطة به بشكل دائم من النظام.",
        isDeleting = isDeleting,
        onDismiss = onDismiss,
        onConfirm = {
            isDeleting = true
            onConfirm()
        }
    )
}

/**
 * Dedicated Confirmation Dialog for Deleting a Chat Conversation.
 */
@Composable
fun DeleteChatConfirmationDialog(
    chat: ChatEntity,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    var isDeleting by remember { mutableStateOf(false) }

    RecordDeleteConfirmationDialog(
        recordType = DeleteRecordType.CHAT,
        title = "تأكيد حذف المحادثة",
        subtitle = "سيتم مسح المحادثة بالكامل وسجل الرسائل المرتبطة",
        itemName = chat.contactName,
        details = listOf(
            RecordDetailItem("رقم الهاتف / المعرف:", if (chat.contactId.isNotBlank()) chat.contactId else "مستخدم"),
            RecordDetailItem("حالة التوثيق:", if (chat.contactVerified) "موثق معتمد ️" else "عادي"),
            RecordDetailItem("آخر رسالة:", chat.lastMessage ?: "لا توجد رسائل")
        ),
        warningMessage = "سيتم حذف كافة الرسائل والوسائط والمستندات المرسلة في هذه المحادثة نهائياً.",
        isDeleting = isDeleting,
        onDismiss = onDismiss,
        onConfirm = {
            isDeleting = true
            onConfirm()
        }
    )
}

/**
 * Dedicated Confirmation Dialog for Deleting an Invoice / Settlement / Transaction.
 */
@Composable
fun DeleteInvoiceConfirmationDialog(
    invoiceTitle: String,
    invoiceNumber: String,
    partyName: String,
    amount: Double,
    goldWeight: Double? = null,
    goldKarat: String? = null,
    dateMillis: Long = System.currentTimeMillis(),
    recordType: DeleteRecordType = DeleteRecordType.INVOICE,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    var isDeleting by remember { mutableStateOf(false) }
    val formattedDate = remember(dateMillis) {
        SimpleDateFormat("yyyy/MM/dd - hh:mm a", Locale.getDefault()).format(Date(dateMillis))
    }

    val detailsList = mutableListOf(
        RecordDetailItem("رقم الفاتورة:", invoiceNumber),
        RecordDetailItem("الطرف المعني:", partyName),
        RecordDetailItem("المبلغ الإجمالي:", GoldUtils.formatCurrency(amount), highlightColor = adaptiveGoldPrimary),
        RecordDetailItem("التاريخ والوقت:", formattedDate)
    )

    if (goldWeight != null && goldWeight > 0) {
        detailsList.add(
            RecordDetailItem("وزن الذهب والعيار:", "${GoldUtils.formatWeight(goldWeight)} (عيار ${goldKarat ?: "21"})", highlightColor = GoldPrimary)
        )
    }

    RecordDeleteConfirmationDialog(
        recordType = recordType,
        title = if (recordType == DeleteRecordType.TRANSACTION) "تأكيد حذف صفقة الذهب" else "تأكيد حذف الفاتورة",
        subtitle = "حذف سجل المعاملة المالية أو الفاتورة المحاسبية",
        itemName = invoiceTitle,
        details = detailsList,
        warningMessage = "سيتم شطب هذه الفاتورة من قيود الحسابات وسجلات الأرصدة المرتبطة بها نهائياً.",
        isDeleting = isDeleting,
        onDismiss = onDismiss,
        onConfirm = {
            isDeleting = true
            onConfirm()
        }
    )
}

@Composable
fun DeleteInvoiceConfirmationDialog(
    invoiceNumber: String,
    partyName: String,
    amountFormatted: String,
    detailsSubtitle: String,
    recordType: DeleteRecordType = DeleteRecordType.INVOICE,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    var isDeleting by remember { mutableStateOf(false) }

    val detailsList = listOf(
        RecordDetailItem("رقم الفاتورة:", invoiceNumber),
        RecordDetailItem("الطرف المعني:", partyName),
        RecordDetailItem("المبلغ الإجمالي:", amountFormatted, highlightColor = adaptiveGoldPrimary)
    )

    RecordDeleteConfirmationDialog(
        recordType = recordType,
        title = if (recordType == DeleteRecordType.TRANSACTION) "تأكيد حذف صفقة الذهب" else "تأكيد حذف الفاتورة",
        subtitle = detailsSubtitle,
        itemName = "فاتورة $invoiceNumber",
        details = detailsList,
        warningMessage = "سيتم شطب هذه الفاتورة وسجلاتها من قيود الحسابات بشكل نهائي.",
        isDeleting = isDeleting,
        onDismiss = onDismiss,
        onConfirm = {
            isDeleting = true
            onConfirm()
        }
    )
}
