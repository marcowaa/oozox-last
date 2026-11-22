package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AssignmentInd
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.BubbleChart
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.GTranslate
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PhoneDisabled
import androidx.compose.material.icons.filled.PhoneForwarded
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.ChatAccountEntity
import com.example.data.model.ChatEntity
import com.example.data.model.HashtagAccountEntity
import com.example.data.model.HashtagEntryDirection
import com.example.data.model.HashtagEntryEntity
import com.example.data.model.HashtagEntryType
import com.example.data.model.MessageEntity
import com.example.data.model.MessageType
import com.example.data.model.UserEntity
import com.example.data.model.UserType
import com.example.ui.components.CallType
import com.example.ui.components.MediaChooserKind
import com.example.ui.components.OozoxAvatarImage
import com.example.ui.components.VoiceDictationMode
import com.example.ui.spatial.ChatSpatialMode
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.OozoxTeal
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusOrange
import com.example.ui.theme.adaptiveGoldPrimary
import com.example.ui.theme.adaptiveOnPrimary
import com.example.util.CallPrivacyManager
import com.example.util.GoldUtils
import com.example.util.HashtagUtils
import com.example.util.MessengerBubbleManager
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChatAttachmentActionsSheet(
    isGroup: Boolean,
    onDismiss: () -> Unit,
    onPickDocument: () -> Unit,
    onPickMediaKind: (MediaChooserKind) -> Unit,
    onOpenGoldDeal: () -> Unit,
    onOpenVoiceBusinessAgent: () -> Unit,
    onOpenVoiceAccounting: () -> Unit,
    onOpenVoiceDictation: (VoiceDictationMode) -> Unit,
    onOpenQuickHashtag: () -> Unit,
    onOpenCreateTask: () -> Unit,
    onOpenAddGroupMembers: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = {
                        onDismiss()
                        onPickDocument()
                    },
                    shape = CircleShape,
                    color = Color(0xFF3B82F6).copy(alpha = 0.16f),
                    border = BorderStroke(1.2.dp, Color(0xFF3B82F6).copy(alpha = 0.45f)),
                    modifier = Modifier.size(58.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.AttachFile,
                            contentDescription = "مستند أو ملف",
                            tint = Color(0xFF3B82F6),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Surface(
                    onClick = {
                        onDismiss()
                        onPickMediaKind(MediaChooserKind.PHOTO)
                    },
                    shape = CircleShape,
                    color = Color(0xFF10B981).copy(alpha = 0.16f),
                    border = BorderStroke(1.2.dp, Color(0xFF10B981).copy(alpha = 0.45f)),
                    modifier = Modifier
                        .size(58.dp)
                        .testTag("chat_attach_photo_btn")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = "إضافة صورة",
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Surface(
                    onClick = {
                        onDismiss()
                        onPickMediaKind(MediaChooserKind.VIDEO)
                    },
                    shape = CircleShape,
                    color = Color(0xFFF59E0B).copy(alpha = 0.16f),
                    border = BorderStroke(1.2.dp, Color(0xFFF59E0B).copy(alpha = 0.45f)),
                    modifier = Modifier
                        .size(58.dp)
                        .testTag("chat_attach_video_btn")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.VideoLibrary,
                            contentDescription = "إضافة فيديو",
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Surface(
                    onClick = {
                        onDismiss()
                        onOpenGoldDeal()
                    },
                    shape = CircleShape,
                    color = GoldPrimary.copy(alpha = 0.16f),
                    border = BorderStroke(1.2.dp, GoldPrimary.copy(alpha = 0.45f)),
                    modifier = Modifier.size(58.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Diamond,
                            contentDescription = "صفقة ذهب",
                            tint = GoldPrimary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = {
                        onDismiss()
                        onOpenVoiceBusinessAgent()
                    },
                    shape = CircleShape,
                    color = OozoxTeal.copy(alpha = 0.18f),
                    border = BorderStroke(1.2.dp, OozoxTeal.copy(alpha = 0.55f)),
                    modifier = Modifier.size(58.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.SupportAgent,
                            contentDescription = "الوكيل الصوتي لإدارة الأعمال",
                            tint = OozoxTeal,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Surface(
                    onClick = {
                        onDismiss()
                        onOpenVoiceAccounting()
                    },
                    shape = CircleShape,
                    color = GoldPrimary.copy(alpha = 0.16f),
                    border = BorderStroke(1.2.dp, GoldPrimary.copy(alpha = 0.45f)),
                    modifier = Modifier.size(58.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Calculate,
                            contentDescription = "قيد محاسبي صوتي",
                            tint = GoldPrimary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Surface(
                    onClick = {
                        onDismiss()
                        onOpenVoiceDictation(VoiceDictationMode.INVOICE)
                    },
                    shape = CircleShape,
                    color = Color(0xFF6366F1).copy(alpha = 0.16f),
                    border = BorderStroke(1.2.dp, Color(0xFF6366F1).copy(alpha = 0.45f)),
                    modifier = Modifier.size(58.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = "إملاء فاتورة",
                            tint = Color(0xFF818CF8),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Surface(
                    onClick = {
                        onDismiss()
                        onOpenVoiceDictation(VoiceDictationMode.PROJECT_NOTES)
                    },
                    shape = CircleShape,
                    color = Color(0xFF8B5CF6).copy(alpha = 0.16f),
                    border = BorderStroke(1.2.dp, Color(0xFF8B5CF6).copy(alpha = 0.45f)),
                    modifier = Modifier.size(58.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.EditNote,
                            contentDescription = "إملاء ملاحظات",
                            tint = Color(0xFFA78BFA),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }

            Surface(
                onClick = {
                    onDismiss()
                    onOpenQuickHashtag()
                },
                shape = RoundedCornerShape(14.dp),
                color = GoldPrimary.copy(alpha = 0.14f),
                border = BorderStroke(1.2.dp, GoldPrimary.copy(alpha = 0.55f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("chat_quick_hashtag_btn")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text("#", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = GoldPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("إدراج هاشتاج أو فاتورة مالية", fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = GoldPrimary)
                }
            }

            Surface(
                onClick = {
                    onDismiss()
                    onOpenCreateTask()
                },
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF10B981).copy(alpha = 0.14f),
                border = BorderStroke(1.2.dp, Color(0xFF10B981).copy(alpha = 0.55f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("chat_plus_assign_task_btn")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AssignmentInd,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("إسناد مهمة", fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = Color(0xFF10B981))
                }
            }

            if (isGroup) {
                Surface(
                    onClick = {
                        onDismiss()
                        onOpenAddGroupMembers()
                    },
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF3B82F6).copy(alpha = 0.14f),
                    border = BorderStroke(1.2.dp, Color(0xFF3B82F6).copy(alpha = 0.55f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("chat_plus_add_member_btn")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = null,
                            tint = Color(0xFF3B82F6),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("إضافة أعضاء للمجموعة", fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = Color(0xFF3B82F6))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChatLedgerSheet(
    chatId: String,
    accounts: List<ChatAccountEntity>,
    hashtags: List<HashtagAccountEntity>,
    allEntries: List<HashtagEntryEntity>,
    onDismiss: () -> Unit,
    onOpenNewAccount: () -> Unit,
    onOpenNewEntry: (hashtagId: String, accountId: String) -> Unit,
    onCreateHashtagAccount: (chatId: String, accountId: String, tag: String) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "دفتر حسابات المحادثة",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = GoldPrimary
                )

                Button(
                    onClick = onOpenNewAccount,
                    colors = ButtonDefaults.buttonColors(containerColor = adaptiveGoldPrimary, contentColor = adaptiveOnPrimary)
                ) {
                    Text("حساب جديد +", color = adaptiveOnPrimary, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (accounts.isEmpty()) {
                Text(
                    text = "لا توجد حسابات مسجلة في هذه المحادثة بعد.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxHeight(0.6f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(accounts, key = { it.id }) { acc ->
                        val accHashtags = hashtags.filter { it.accountId == acc.id }
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(acc.name, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    Text(acc.status, fontSize = 12.sp, color = GoldAccent)
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                accHashtags.forEach { h ->
                                    val hEntries = allEntries.filter { it.hashtagId == h.id }
                                    val totals = HashtagUtils.calculateHashtagTotals(hEntries)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("#${h.tag}", color = GoldPrimary, fontWeight = FontWeight.SemiBold)
                                        Text(
                                            "المتبقي: ${GoldUtils.formatCurrency(totals.remaining)}",
                                            color = if (totals.remaining > 0) StatusOrange else StatusGreen,
                                            fontSize = 13.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            val firstHash = accHashtags.firstOrNull()
                                            if (firstHash != null) {
                                                onOpenNewEntry(firstHash.id, acc.id)
                                            } else {
                                                onCreateHashtagAccount(chatId, acc.id, "عام")
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = adaptiveGoldPrimary,
                                            contentColor = adaptiveOnPrimary
                                        ),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("+ فاتورة / دفع", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun ChatNewAccountDialog(
    onDismiss: () -> Unit,
    onCreateAccount: (String) -> Unit
) {
    var accName by remember { mutableStateOf("") }
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
                Brush.verticalGradient(
                    colors = listOf(
                        GoldPrimary.copy(alpha = 0.6f),
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                    )
                )
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(GoldPrimary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = GoldPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "إضافة حساب مالي جديد",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "إنشاء محفظة حسابات مستقلة داخل المحادثة",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                OutlinedTextField(
                    value = accName,
                    onValueChange = { accName = it },
                    label = { Text("اسم الحساب (مثال: توريدات ذهب 2026)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldPrimary,
                        focusedLabelColor = GoldPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                    ) {
                        Text("إلغاء", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Button(
                        onClick = {
                            if (accName.isNotBlank()) {
                                onCreateAccount(accName.trim())
                            }
                        },
                        enabled = accName.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = adaptiveGoldPrimary,
                            contentColor = adaptiveOnPrimary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                    ) {
                        Text("إنشاء الحساب", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
internal fun ChatNewEntryDialog(
    onDismiss: () -> Unit,
    onSaveEntry: (entryType: HashtagEntryType, desc: String, amount: Double, direction: HashtagEntryDirection) -> Unit
) {
    var desc by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    var entryType by remember { mutableStateOf(HashtagEntryType.INVOICE) }

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
                Brush.verticalGradient(
                    colors = listOf(
                        GoldPrimary.copy(alpha = 0.6f),
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                    )
                )
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(GoldPrimary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = GoldPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "إضافة فاتورة / دفعة مالية",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "تسجيل قيد مالي مباشر بالدفتر",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = entryType == HashtagEntryType.INVOICE,
                        onClick = { entryType = HashtagEntryType.INVOICE },
                        label = { Text("فاتورة (مدين)") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = entryType == HashtagEntryType.PAYMENT,
                        onClick = { entryType = HashtagEntryType.PAYMENT },
                        label = { Text("دفعة (دائن)") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("الوصف / بيان المعاملة") },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldPrimary,
                        focusedLabelColor = GoldPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("المبلغ (بالجنيه المصري)") },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldPrimary,
                        focusedLabelColor = GoldPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                    ) {
                        Text("إلغاء", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Button(
                        onClick = {
                            val amt = amountStr.toDoubleOrNull()
                            if (desc.isNotBlank() && amt != null && amt > 0) {
                                val dir = if (entryType == HashtagEntryType.INVOICE) HashtagEntryDirection.DEBIT else HashtagEntryDirection.CREDIT
                                onSaveEntry(entryType, desc.trim(), amt, dir)
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = adaptiveGoldPrimary,
                            contentColor = adaptiveOnPrimary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                    ) {
                        Text("حفظ المعاملة", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
internal fun ChatCallPrivacyBlockedDialog(
    reason: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Default.PhoneDisabled, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(32.dp))
        },
        title = {
            Text("خصوصية المكالمات", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        },
        text = {
            Text(
                text = reason,
                fontSize = 13.5.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = adaptiveGoldPrimary, contentColor = adaptiveOnPrimary)
            ) {
                Text("حسناً", fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
internal fun ChatZoxCallBlockedDialog(
    onDismiss: () -> Unit
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
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFEF4444).copy(alpha = 0.6f),
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                    )
                )
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEF4444).copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PhoneDisabled,
                        contentDescription = null,
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(28.dp)
                    )
                }

                Text(
                    text = "المكالمات الواردة محظورة لحساب ZOX",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "حساب الإدارة العليا والدعم الفني (ZOX) مخصص للمراسلات النصية المشفرة وتوثيق السجلات والقيود المالية فقط لضمان سرية وحفظ العمليات والردود الرسمية المعتمدة.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Center
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Security,
                            contentDescription = null,
                            tint = GoldPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "يتم الرد الفوري على استفساراتك وتوجيه طلباتك عبر الدردشة النصية المباشرة.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("إغلاق", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = adaptiveGoldPrimary,
                            contentColor = adaptiveOnPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("مراسلة نصية", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
internal fun ChatPermissionRationaleDialog(
    icon: ImageVector,
    title: String,
    body: String,
    onDismiss: () -> Unit,
    onGrant: () -> Unit
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
                Brush.verticalGradient(
                    colors = listOf(
                        GoldPrimary.copy(alpha = 0.6f),
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                    )
                )
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(GoldPrimary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = GoldPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = body,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Center
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("إلغاء", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = onGrant,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = adaptiveGoldPrimary,
                            contentColor = adaptiveOnPrimary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("منح الإذن", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
internal fun ChatMessageOptionsDialog(
    targetMsg: MessageEntity,
    onDismiss: () -> Unit,
    onToggleReaction: (messageId: String, currentReaction: String?, emoji: String) -> Unit,
    onReply: (MessageEntity) -> Unit,
    onForward: (MessageEntity) -> Unit,
    onCopiedNotice: (String) -> Unit,
    onTranslate: (MessageEntity) -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val emojis = listOf("معتمد", "تم الاطلاع", "موافق", "قيد المراجعة")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            shadowElevation = 12.dp,
            border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "خيارات الرسالة والتفاعل",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "إغلاق",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 6.dp, vertical = 6.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        emojis.forEach { emoji ->
                            val isSelected = targetMsg.reaction == emoji
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) GoldPrimary.copy(alpha = 0.25f) else Color.Transparent)
                                    .border(
                                        width = if (isSelected) 1.5.dp else 0.dp,
                                        color = if (isSelected) GoldPrimary else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        onToggleReaction(targetMsg.id, targetMsg.reaction, emoji)
                                        onDismiss()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = emoji, fontSize = 18.sp)
                            }
                        }
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = when (targetMsg.type) {
                            MessageType.VOICE.name -> "تسجيل صوتي"
                            MessageType.IMAGE.name -> "صورة مرفقة"
                            else -> targetMsg.content.take(80)
                        },
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = adaptiveGoldPrimary,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clickable {
                                onDismiss()
                                onReply(targetMsg)
                            }
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = null, tint = adaptiveOnPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("رد ↩️", color = adaptiveOnPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clickable {
                                onDismiss()
                                onForward(targetMsg)
                            }
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("توجيه ↗️", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clickable {
                                clipboardManager.setText(AnnotatedString(targetMsg.content))
                                onCopiedNotice("تم نسخ النص")
                                onDismiss()
                            }
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("نسخ", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }

                if (targetMsg.type == MessageType.TEXT.name && targetMsg.content.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp)
                            .clickable {
                                onDismiss()
                                onTranslate(targetMsg)
                            }
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Translate,
                                contentDescription = null,
                                tint = GoldPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ترجمة سياقية ذكية (Gemini AI)",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                if (!targetMsg.reaction.isNullOrBlank()) {
                    TextButton(
                        onClick = {
                            onToggleReaction(targetMsg.id, targetMsg.reaction, targetMsg.reaction)
                            onDismiss()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(32.dp)
                    ) {
                        Text("إزالة التفاعل (${targetMsg.reaction})", color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
internal fun ChatContactProfileDialog(
    chat: ChatEntity,
    contactUser: UserEntity?,
    onDismiss: () -> Unit,
    onStartCall: (CallType) -> Unit,
    onNavigateToProfile: (String) -> Unit,
    onOpenChatSettings: () -> Unit
) {
    val context = LocalContext.current
    val canViewPublicProfile = !chat.isGroup && (contactUser?.showSkillsPublicly != false)
    val hasDirectPhone = !chat.isGroup && !contactUser?.phone.isNullOrBlank()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 420.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 18.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (chat.isGroup) "معلومات المجموعة" else "بيانات جهة الاتصال",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "إغلاق",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                OozoxAvatarImage(
                    model = contactUser?.avatarUri,
                    name = chat.contactName,
                    size = 76.dp,
                    isGroup = chat.isGroup,
                    isTrader = chat.contactType == UserType.GOLD_TRADER.name,
                    contentDescription = chat.contactName
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = chat.contactName,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (chat.contactVerified) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                Icons.Default.Verified,
                                contentDescription = "حساب موثق",
                                tint = GoldPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    val subtitleText = when {
                        chat.isGroup -> "مجموعة عمل"
                        !contactUser?.jobTitle.isNullOrBlank() -> contactUser?.jobTitle!!
                        chat.contactType == UserType.GOLD_TRADER.name -> "تاجر ذهب ومعادن"
                        else -> "حساب أعمال"
                    }
                    Text(
                        text = subtitleText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = GoldPrimary
                    )

                    if (!chat.isGroup && (!contactUser?.city.isNullOrBlank() || (contactUser?.totalReviewsCount ?: 0) > 0)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (!contactUser?.city.isNullOrBlank()) {
                                Text(
                                    text = contactUser?.city!!,
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if ((contactUser?.totalReviewsCount ?: 0) > 0) {
                                Text(
                                    text = "★ ${String.format(Locale.US, "%.1f", contactUser?.ratingScore ?: 0f)}",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "خيارات الاتصال",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            onClick = {
                                onDismiss()
                                onStartCall(CallType.AUDIO)
                            },
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.35f)),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 52.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Default.Call,
                                    contentDescription = "مكالمة صوتية",
                                    tint = Color(0xFF059669),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "صوتية",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF059669)
                                )
                            }
                        }

                        Surface(
                            onClick = {
                                onDismiss()
                                onStartCall(CallType.VIDEO)
                            },
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 52.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Default.Videocam,
                                    contentDescription = "مكالمة فيديو",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "فيديو",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        if (hasDirectPhone) {
                            Surface(
                                onClick = {
                                    onDismiss()
                                    try {
                                        val dialIntent = Intent(
                                            Intent.ACTION_DIAL,
                                            Uri.parse("tel:${contactUser?.phone}")
                                        )
                                        context.startActivity(dialIntent)
                                    } catch (_: Exception) {}
                                },
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 52.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        Icons.Default.PhoneForwarded,
                                        contentDescription = "اتصال هاتفي",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "هاتفي",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                if (canViewPublicProfile) {
                    Button(
                        onClick = {
                            onDismiss()
                            onNavigateToProfile(chat.contactId)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = adaptiveGoldPrimary,
                            contentColor = adaptiveOnPrimary
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "الصفحة الشخصية",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onOpenChatSettings()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "إعدادات المحادثة والحظر",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChatMessengerOptionsSheet(
    chat: ChatEntity,
    contactUser: UserEntity?,
    isZoxAdminChat: Boolean,
    activeGroupMembersCount: Int,
    accountsCount: Int,
    messagesDisplayCount: Int,
    activeSpatialMode: ChatSpatialMode,
    isAutoTranslateEnabled: Boolean,
    translateMyMessagesToo: Boolean,
    isUserBlocked: Boolean,
    onDismiss: () -> Unit,
    onCallAttempt: (CallType) -> Unit,
    onCallPrivacyBlocked: (String) -> Unit,
    onZoxCallBlocked: () -> Unit,
    onNavigateToProfile: (String) -> Unit,
    onOpenContactProfile: () -> Unit,
    onBackAfterBubble: () -> Unit,
    onOpenGroupMembers: () -> Unit,
    onOpenChatSettings: () -> Unit,
    onToggleTopModesRail: () -> Unit,
    onAutoTranslateChange: (Boolean) -> Unit,
    onTranslateMyMessagesChange: (Boolean) -> Unit,
    onOpenGoldDeal: () -> Unit,
    onOpenLedgerSheet: () -> Unit,
    onOpenClearingDialog: () -> Unit,
    onExportPdf: () -> Unit,
    onOpenVoiceBusinessAgent: () -> Unit,
    onOpenAIAccountant: () -> Unit
) {
    val context = LocalContext.current
    val optionsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val canViewPublicProfile = !chat.isGroup && (contactUser?.showSkillsPublicly != false)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = optionsSheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.outlineVariant) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(modifier = Modifier.size(68.dp)) {
                OozoxAvatarImage(
                    model = contactUser?.avatarUri,
                    name = chat.contactName,
                    size = 68.dp,
                    isGroup = chat.isGroup,
                    isTrader = chat.contactType == UserType.GOLD_TRADER.name,
                    contentDescription = chat.contactName
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF10B981))
                        .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = chat.contactName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (chat.contactVerified) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "موثق",
                            tint = GoldPrimary,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
                Text(
                    text = when {
                        isZoxAdminChat -> "الإدارة الرسمية"
                        chat.isGroup -> "مجموعة عمل نشطة"
                        !contactUser?.jobTitle.isNullOrBlank() -> contactUser?.jobTitle!!
                        chat.contactType == UserType.GOLD_TRADER.name -> "تاجر ذهب ومعادن معتمد"
                        else -> "حساب أعمال"
                    },
                    fontSize = 12.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!chat.isGroup && (!contactUser?.city.isNullOrBlank() || (contactUser?.totalReviewsCount ?: 0) > 0)) {
                    val detailsLine = buildList {
                        contactUser?.city?.takeIf { it.isNotBlank() }?.let { add(it) }
                        if ((contactUser?.totalReviewsCount ?: 0) > 0) {
                            add("تقييم ${String.format(Locale.US, "%.1f", contactUser?.ratingScore ?: 0f)}")
                        }
                    }.joinToString(" • ")
                    if (detailsLine.isNotBlank()) {
                        Text(
                            text = detailsLine,
                            fontSize = 11.5.sp,
                            color = GoldPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MessengerQuickCircleAction(
                    icon = if (isZoxAdminChat) Icons.Default.PhoneDisabled else Icons.Default.Call,
                    label = "اتصال",
                    enabled = !isZoxAdminChat,
                    onClick = {
                        onDismiss()
                        val (allowed, reason) = CallPrivacyManager.checkCallAllowed(
                            context = context,
                            isContactSaved = !chat.isGroup,
                            callerName = chat.contactName
                        )
                        if (!allowed && reason != null) {
                            onCallPrivacyBlocked(reason)
                        } else if (isZoxAdminChat) {
                            onZoxCallBlocked()
                        } else {
                            onCallAttempt(CallType.AUDIO)
                        }
                    }
                )
                MessengerQuickCircleAction(
                    icon = if (isZoxAdminChat) Icons.Default.VideocamOff else Icons.Default.Videocam,
                    label = "فيديو",
                    enabled = !isZoxAdminChat,
                    onClick = {
                        onDismiss()
                        val (allowed, reason) = CallPrivacyManager.checkCallAllowed(
                            context = context,
                            isContactSaved = !chat.isGroup,
                            callerName = chat.contactName
                        )
                        if (!allowed && reason != null) {
                            onCallPrivacyBlocked(reason)
                        } else if (isZoxAdminChat) {
                            onZoxCallBlocked()
                        } else {
                            onCallAttempt(CallType.VIDEO)
                        }
                    }
                )
                MessengerQuickCircleAction(
                    icon = Icons.Default.Person,
                    label = "الملف",
                    enabled = true,
                    onClick = {
                        onDismiss()
                        if (canViewPublicProfile) {
                            onNavigateToProfile(chat.contactId)
                        } else {
                            onOpenContactProfile()
                        }
                    }
                )
                MessengerQuickCircleAction(
                    icon = Icons.Default.BubbleChart,
                    label = "فقاعة",
                    enabled = true,
                    onClick = {
                        onDismiss()
                        MessengerBubbleManager.setBubbleFeatureEnabled(context, true)
                        MessengerBubbleManager.showBubble(
                            chatId = chat.id,
                            contactName = chat.contactName,
                            contactAvatarUri = contactUser?.avatarUri,
                            contactRole = if (chat.contactType == UserType.GOLD_TRADER.name) "تاجر ذهب معتمد" else "حساب أعمال",
                            messageText = chat.lastMessage ?: "",
                            autoExpand = true,
                            isOnline = true
                        )
                        onBackAfterBubble()
                    }
                )
            }

            if (chat.isGroup) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "إدارة المجموعة والأعضاء",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = GoldPrimary.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("messenger_options_group_members_row")
                    ) {
                        MessengerOptionItemRow(
                            icon = Icons.Default.GroupAdd,
                            title = "أعضاء المجموعة وإضافة أعضاء",
                            subtitle = "$activeGroupMembersCount عضو حالي • إضافة أو إدارة الصلاحيات",
                            onClick = {
                                onDismiss()
                                onOpenGroupMembers()
                            }
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "التخصيص والترجمة",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        MessengerOptionItemRow(
                            icon = Icons.Default.Palette,
                            title = "المظهر والألوان",
                            subtitle = "الخلفية، الفقاعات، ولون الخط",
                            onClick = {
                                onDismiss()
                                onOpenChatSettings()
                            }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        MessengerOptionItemRow(
                            icon = Icons.Default.Tune,
                            title = "تصنيف المحادثة",
                            subtitle = activeSpatialMode.label,
                            onClick = {
                                onDismiss()
                                onToggleTopModesRail()
                            }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        MessengerOptionSwitchRow(
                            icon = Icons.Default.Translate,
                            title = "ترجمة الطرف الآخر",
                            checked = isAutoTranslateEnabled,
                            onCheckedChange = onAutoTranslateChange
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        MessengerOptionSwitchRow(
                            icon = Icons.Default.GTranslate,
                            title = "ترجمة رسائلي أيضاً",
                            checked = translateMyMessagesToo,
                            onCheckedChange = onTranslateMyMessagesChange
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "الأعمال والتجارة",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        MessengerOptionItemRow(
                            icon = Icons.Default.MonetizationOn,
                            title = "صفقة ذهب مباشرة",
                            subtitle = "إدراج فاتورة أو عيار",
                            onClick = {
                                onDismiss()
                                onOpenGoldDeal()
                            }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        MessengerOptionItemRow(
                            icon = Icons.Default.Book,
                            title = "دفتر الحسابات والقيود",
                            subtitle = "$accountsCount حسابات مالية",
                            onClick = {
                                onDismiss()
                                onOpenLedgerSheet()
                            }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        MessengerOptionItemRow(
                            icon = Icons.Default.Balance,
                            title = "تقاص وتصفية الحساب",
                            subtitle = "تسوية الأرصدة والموازين",
                            onClick = {
                                onDismiss()
                                onOpenClearingDialog()
                            }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        MessengerOptionItemRow(
                            icon = Icons.Default.PictureAsPdf,
                            title = "تصدير المحادثة PDF",
                            subtitle = "$messagesDisplayCount رسالة",
                            onClick = {
                                onDismiss()
                                onExportPdf()
                            }
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "المساعدة والخصوصية",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        MessengerOptionItemRow(
                            icon = Icons.Default.RecordVoiceOver,
                            title = "الوكيل الصوتي للأعمال",
                            subtitle = "تسجيل وتنفيذ الأوامر",
                            onClick = {
                                onDismiss()
                                onOpenVoiceBusinessAgent()
                            }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        MessengerOptionItemRow(
                            icon = Icons.Default.Analytics,
                            title = "المحاسب المالي المتقدم",
                            subtitle = "مراجعة المعاملات والوفر",
                            onClick = {
                                onDismiss()
                                onOpenAIAccountant()
                            }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        MessengerOptionItemRow(
                            icon = Icons.Default.Security,
                            title = "إعدادات المحادثة والحظر",
                            subtitle = if (isUserBlocked) "محظور حالياً" else "إدارة الخصوصية والحظر",
                            onClick = {
                                onDismiss()
                                onOpenContactProfile()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MessengerQuickCircleAction(
    icon: ImageVector,
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = if (enabled) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
            modifier = Modifier.size(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (enabled) GoldPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun MessengerOptionItemRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = GoldPrimary,
            modifier = Modifier.size(20.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun MessengerOptionSwitchRow(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = GoldPrimary,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = title,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = GoldPrimary
            )
        )
    }
}
