package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.fragment.app.FragmentActivity
import com.example.data.model.UserEntity
import com.example.ui.components.AppToastType
import com.example.ui.components.LocalAppToast
import com.example.ui.theme.*
import com.example.util.AppLanguage
import com.example.util.BiometricHelper
import com.example.util.BiometricStatus
import com.example.util.CallPrivacy
import com.example.util.LocalAppLanguage
import com.example.util.Translations
import com.example.util.tr
import kotlinx.coroutines.launch

@Composable
internal fun ProfileVerificationDialog(
    initialShopAddress: String,
    onSubmit: (nationalId: String, commercialReg: String, shopAddress: String, notes: String) -> Unit,
    onDismiss: () -> Unit
) {
    var natId by remember { mutableStateOf("") }
    var commReg by remember { mutableStateOf("") }
    var shopAddr by remember { mutableStateOf(initialShopAddress) }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("طلب توثيق الحساب", color = GoldPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "يرجى إدخال البيانات الرسمية لتوثيق حسابك:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = natId,
                    onValueChange = { natId = it },
                    label = { Text("الرقم القومي", style = MaterialTheme.typography.bodySmall) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    textStyle = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = commReg,
                    onValueChange = { commReg = it },
                    label = { Text("السجل التجاري", style = MaterialTheme.typography.bodySmall) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    textStyle = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = shopAddr,
                    onValueChange = { shopAddr = it },
                    label = { Text("عنوان النشاط", style = MaterialTheme.typography.bodySmall) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    textStyle = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات", style = MaterialTheme.typography.bodySmall) },
                    minLines = 2,
                    maxLines = 3,
                    shape = RoundedCornerShape(12.dp),
                    textStyle = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                ) {
                    Text("إلغاء")
                }

                Button(
                    onClick = {
                        if (natId.isNotBlank() || commReg.isNotBlank()) {
                            onSubmit(natId, commReg, shopAddr, notes)
                            onDismiss()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                ) {
                    Text("إرسال الطلب", color = MaterialTheme.colorScheme.background, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {},
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
internal fun ProfileCoverPickerDialog(
    userName: String,
    selectedCover: String?,
    currentLanguage: AppLanguage,
    onApplyCover: (String) -> Unit,
    onPickFromGallery: () -> Unit,
    onDismiss: () -> Unit
) {
    var previewCover by remember(selectedCover) { mutableStateOf(selectedCover ?: "تدرج ذهبي ملكي") }

    val coverPresets = remember(currentLanguage, Translations.catalogVersion) {
        listOf(
            Triple(
                "تدرج ذهبي ملكي",
                Translations.get("profile.cover_gold_t", currentLanguage),
                Translations.get("profile.cover_gold_d", currentLanguage)
            ),
            Triple(
                "خلفية فضية فاخرة",
                Translations.get("profile.cover_silver_t", currentLanguage),
                Translations.get("profile.cover_silver_d", currentLanguage)
            ),
            Triple(
                "نمط المقاولات والعقارات",
                Translations.get("profile.cover_teal_t", currentLanguage),
                Translations.get("profile.cover_teal_d", currentLanguage)
            )
        )
    }

    val previewGradient = when (previewCover) {
        "خلفية فضية فاخرة" -> listOf(Color(0xFF334155), Color(0xFF1E293B), Color(0xFF0F172A))
        "نمط المقاولات والعقارات" -> listOf(Color(0xFF0F766E), Color(0xFF115E59), Color(0xFF134E4A))
        else -> listOf(Color(0xFFD4AF37), Color(0xFF9A7B1C), Color(0xFF1E1B14))
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        CompositionLocalProvider(
            LocalLayoutDirection provides currentLanguage.direction,
            LocalAppLanguage provides currentLanguage
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .widthIn(max = 520.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tr("profile.cover_title"),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = tr("profile.cover_sub"),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = tr("common.close"),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(108.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Brush.linearGradient(previewGradient))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.align(Alignment.BottomStart),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.22f))
                                    .border(1.5.dp, Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = userName.trim().take(1).ifBlank { "O" },
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = userName,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = tr("profile.cover_live_preview"),
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 11.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        coverPresets.forEach { (key, title, subtitle) ->
                            val isSelected = previewCover == key
                            val swatchColors = when (key) {
                                "خلفية فضية فاخرة" -> listOf(Color(0xFF64748B), Color(0xFF1E293B))
                                "نمط المقاولات والعقارات" -> listOf(Color(0xFF14B8A6), Color(0xFF0F766E))
                                else -> listOf(Color(0xFFFDE047), Color(0xFFB48811))
                            }

                            Surface(
                                onClick = { previewCover = key },
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) adaptiveGoldPrimary.copy(alpha = 0.10f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                border = BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) adaptiveGoldPrimary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 64.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Brush.linearGradient(swatchColors))
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = title,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = subtitle,
                                            fontSize = 11.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = tr("common.selected"),
                                            tint = adaptiveGoldPrimary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = onPickFromGallery,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, adaptiveGoldPrimary.copy(alpha = 0.7f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = null,
                            tint = adaptiveGoldPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = tr("profile.cover_pick_gallery"),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = adaptiveGoldPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                        ) {
                            Text(
                                text = tr("common.cancel"),
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Button(
                            onClick = {
                                onApplyCover(previewCover)
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = adaptiveGoldPrimary,
                                contentColor = adaptiveOnPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                        ) {
                            Text(
                                text = tr("profile.cover_apply"),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun ProfileCallPrivacyDialog(
    currentMode: CallPrivacy,
    onSelectPrivacy: (CallPrivacy) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Default.PhoneCallback, contentDescription = null, tint = adaptiveGoldPrimary, modifier = Modifier.size(28.dp))
        },
        title = {
            Text("خصوصية المكالمات الصوتية والمرئية", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "حدد من يمكنه الاتصال بك صوتياً ومرئياً عبر منصة OOZOX:",
                    fontSize = 12.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                CallPrivacy.entries.forEach { option ->
                    val isSelected = option == currentMode
                    Surface(
                        onClick = {
                            onSelectPrivacy(option)
                            onDismiss()
                            Toast.makeText(context, "تم ضبط خصوصية المكالمات: ${option.titleAr}", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) adaptiveGoldPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = BorderStroke(1.dp, if (isSelected) adaptiveGoldPrimary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(selectedColor = adaptiveGoldPrimary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = option.titleAr,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (isSelected) GoldPrimary else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = option.descAr,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق")
            }
        }
    )
}

@Composable
internal fun ProfileAvatarPickerDialog(
    hasCustomAvatar: Boolean,
    onCapturePhotoClick: () -> Unit,
    onPickGalleryClick: () -> Unit,
    onSelectPresetEmoji: (String) -> Unit,
    onRemoveAvatar: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = adaptiveGoldPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("تحديث الصورة الشخصية", color = adaptiveGoldPrimary, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "التقط صورة جديدة بكاميرا جهازك أو اختر من الاستوديو أو الرموز:",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(
                    onClick = onCapturePhotoClick,
                    colors = ButtonDefaults.buttonColors(containerColor = adaptiveGoldPrimary, contentColor = adaptiveOnPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("capture_avatar_camera_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "التقاط بالكاميرا",
                        tint = adaptiveOnPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "التقاط صورة شخصية بالكاميرا 📸",
                        color = adaptiveOnPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                OutlinedButton(
                    onClick = onPickGalleryClick,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pick_avatar_gallery_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = "اختيار من المعرض",
                        tint = GoldPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "اختيار صورة من المعرض (Gallery) 🖼️",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))

                Text(
                    text = "أو اختر رمزاً تعبيرياً كأفاتار:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val presets = listOf(
                    "🪙 تاجر ذهب" to "🪙",
                    "👔 مستثمر" to "👔",
                    "🏪 متجر ومحل" to "🏪",
                    "👑 حساب ملكي" to "👑",
                    "💎 بلاتيني" to "💎"
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presets.forEach { (label, emoji) ->
                        SuggestionChip(
                            onClick = { onSelectPresetEmoji(emoji) },
                            label = { Text(label, fontSize = 12.sp) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                labelColor = MaterialTheme.colorScheme.onSurface
                            ),
                            border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.3f))
                        )
                    }
                }

                if (hasCustomAvatar) {
                    TextButton(
                        onClick = onRemoveAvatar,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "إزالة الصورة واستعادة الرمز الافتراضي",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
internal fun ProfileCameraPermissionRationaleDialog(
    onGrantClick: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CameraAlt, contentDescription = null, tint = adaptiveGoldPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("إذن استخدام الكاميرا", color = adaptiveGoldPrimary, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Text(
                text = "يحتاج تطبيق أوزوكس (OOZOX) إلى إذن الوصول للكاميرا ليتمكن من التقاط صورتك الشخصية وتحديث ملفك التعريفي كتاجر أو مستثمر.\n\nيرجى منح الإذن عند المطالبة به للمتابعة.",
                fontSize = 13.sp,
                lineHeight = 20.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                ) {
                    Text("إلغاء", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = onGrantClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = adaptiveGoldPrimary, contentColor = adaptiveOnPrimary),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                ) {
                    Text("منح الإذن", color = adaptiveOnPrimary, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {},
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
internal fun ProfileDeleteAccountDialog(
    onConfirmDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تأكيد حذف الحساب", color = Color(0xFFE53935), fontWeight = FontWeight.Bold) },
        text = {
            Text(
                "وفقاً لسياسة الخصوصية، إن الضغط على تأكيد الحذف سيقوم بإزالة بيانات حسابك وملفات التوثيق والاشتراكات نهائياً من النظام. هل أنت متأكد من المتابعة؟",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                ) {
                    Text("إلغاء")
                }

                Button(
                    onClick = {
                        onDismiss()
                        onConfirmDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                ) {
                    Text("حذف الحساب", color = MaterialTheme.colorScheme.onError, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {},
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
internal fun ProfilePrivacyPolicyDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("سياسة الخصوصية وأمان البيانات", color = adaptiveGoldPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("التزام منصة OOZOX (oozox.com) بخصوصية وأمان المستخدمين:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = adaptiveGoldPrimary)
                Text("1. الهوية والرسالة: تمكين التواصل الفعال وإدارة الأعمال والمحاسبة باحترافية ودقة.", fontSize = 12.sp)
                Text("2. حماية البيانات المالية: تشفير القيود المحاسبية والفواتير وكافة سجلات الأعمال لضمان الأمان.", fontSize = 12.sp)
                Text("3. الخصوصية: بياناتك ملكك وحدك، ولا يتم مشاركتها مع أي جهة خارجية.", fontSize = 12.sp)
                Text("4. التوثيق: جميع العمليات المالية موثقة بالساعة والتاريخ لضمان الرقابة المالية.", fontSize = 12.sp)
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = adaptiveGoldPrimary, contentColor = adaptiveOnPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
            ) {
                Text("موافق وإغلاق", color = adaptiveOnPrimary, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
internal fun ProfileLogoutDialog(
    onConfirmLogout: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "تسجيل الخروج",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Text(
                text = "هل أنت متأكد من رغبتك في تسجيل الخروج؟ سيتم مسح بيانات الجلسة الحالية والعودة لشاشة البداية.",
                fontSize = 13.sp,
                lineHeight = 20.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("cancel_logout_button")
                ) {
                    Text("إلغاء")
                }

                Button(
                    onClick = {
                        onDismiss()
                        onConfirmLogout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("confirm_logout_button")
                ) {
                    Text("تسجيل الخروج", color = MaterialTheme.colorScheme.onError, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {},
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
internal fun ProfileAppLockDialog(
    user: UserEntity,
    onSaveAppLock: (enabled: Boolean, pin: String, biometricEnabled: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val appToast = LocalAppToast.current
    val activity = context as? FragmentActivity
    var pinValue by remember { mutableStateOf(user.appLockPin ?: "") }
    var isEnabledSwitch by remember { mutableStateOf(user.appLockEnabled) }
    var isBiometricSwitch by remember { mutableStateOf(user.biometricAuthEnabled) }
    val bioStatus = remember(context) { BiometricHelper.checkBiometricStatus(context) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isEnabledSwitch) Icons.Default.Fingerprint else Icons.Default.LockOpen,
                    contentDescription = null,
                    tint = adaptiveGoldPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "المصادقة البيومترية ورمز PIN",
                    color = adaptiveGoldPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "تأمين بيانات الصفقات والمحفظة وتداول الذهب بالبصمة أو التعرف على الوجه ورمز PIN عند فتح التطبيق.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "تفعيل القفل الأمني:",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "طلب المصادقة عند كل فتح للتطبيق",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isEnabledSwitch,
                            onCheckedChange = { isEnabledSwitch = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = adaptiveGoldPrimary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }

                if (isEnabledSwitch) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Face, contentDescription = null, tint = adaptiveGoldPrimary, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(Icons.Default.Fingerprint, contentDescription = null, tint = adaptiveGoldPrimary, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "بصمة الإصبع والوجه:",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = when (bioStatus) {
                                                BiometricStatus.AVAILABLE -> "مستشعر البصمة والوجه جاهز 🟢"
                                                BiometricStatus.NOT_ENROLLED -> "لم يتم تسجيل بصمات بالجهاز ⚠️"
                                                BiometricStatus.NO_HARDWARE -> "الجهاز لا يحتوي على مستشعر بيومتري"
                                                else -> "المصادقة البيومترية مدعومة"
                                            },
                                            fontSize = 10.sp,
                                            color = if (bioStatus == BiometricStatus.AVAILABLE) adaptiveStatusGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Switch(
                                    checked = isBiometricSwitch,
                                    onCheckedChange = { isBiometricSwitch = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = adaptiveGoldPrimary,
                                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                                    )
                                )
                            }

                            if (bioStatus == BiometricStatus.AVAILABLE && activity != null) {
                                OutlinedButton(
                                    onClick = {
                                        BiometricHelper.authenticate(
                                            activity = activity,
                                            title = "اختبار المصادقة البيومترية",
                                            subtitle = "بصمة الإصبع أو التعرف على الوجه",
                                            description = "جاري التحقق من عمل المستشعر البيومتري بنجاح",
                                            negativeButtonText = "إلغاء",
                                            onSuccess = {
                                                appToast.showToast("تمت المصادقة البيومترية بنجاح! المستشعر يعمل بكفاءة 🛡️", AppToastType.SUCCESS)
                                            },
                                            onError = { _, err ->
                                                appToast.showToast("تنبيه: $err", AppToastType.WARNING)
                                            },
                                            onFailed = {
                                                appToast.showToast("لم يتم التعرف على البصمة، حاول مجدداً", AppToastType.ERROR)
                                            }
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = adaptiveGoldPrimary)
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = adaptiveGoldPrimary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("تجربة البصمة / الوجه الآن 👆", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = adaptiveGoldPrimary)
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = pinValue,
                        onValueChange = { if (it.length <= 6 && it.all { char -> char.isDigit() }) pinValue = it },
                        label = { Text("رمز PIN (4-6 أرقام)", style = MaterialTheme.typography.bodySmall) },
                        placeholder = { Text("أدخل رمز المرور", style = MaterialTheme.typography.bodySmall) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        textStyle = MaterialTheme.typography.bodyMedium,
                        leadingIcon = { Icon(Icons.Default.Pin, contentDescription = null, tint = adaptiveGoldPrimary, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                ) {
                    Text("إلغاء")
                }

                Button(
                    onClick = {
                        if (isEnabledSwitch && pinValue.length < 4) {
                            appToast.showToast("يرجى إدخال رمز PIN من 4 أرقام على الأقل", AppToastType.WARNING)
                            return@Button
                        }
                        onSaveAppLock(
                            isEnabledSwitch,
                            if (isEnabledSwitch) pinValue else "",
                            isBiometricSwitch
                        )
                        onDismiss()
                        appToast.showToast("تم حفظ إعدادات الأمان", AppToastType.SUCCESS)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                ) {
                    Text("حفظ الأمان", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {},
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
internal fun ProfileMnemonicPhraseDialog(
    userMnemonicSeed: List<String>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val appToast = LocalAppToast.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Key, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("عبارة استرداد الحساب", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "احفظ هذه الكلمات السرية بالترتيب في مكان آمن وغير متصل بالإنترنت. تُستخدم هذه الكلمات كنسخة طوارئ نهائية لاستعادة حسابك وكافة الخزائن:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(
                    color = MaterialTheme.colorScheme.background,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        userMnemonicSeed.chunked(3).forEachIndexed { rowIndex, chunk ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                chunk.forEachIndexed { colIndex, word ->
                                    val idx = rowIndex * 3 + colIndex + 1
                                    Surface(
                                        color = MaterialTheme.colorScheme.surface,
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("$idx.", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(word, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                OutlinedButton(
                    onClick = {
                        val seedString = userMnemonicSeed.joinToString(" ")
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        val clip = ClipData.newPlainText("OOZOX Seed Phrase", seedString)
                        clipboard?.setPrimaryClip(clip)
                        appToast.showToast("تم نسخ عبارة الاسترداد للحافظة", AppToastType.SUCCESS)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("نسخ عبارة الاسترداد", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
            ) {
                Text("تم الحفظ بأمان", fontWeight = FontWeight.Bold)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
internal fun ProfileDatabaseSecurityAuditDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Security, contentDescription = null, tint = OozoxTeal, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("تدقيق أمان قواعد البيانات وحصانة الحقن 🛡️", color = OozoxTeal, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "كافة جداول Room والعمليات المالية في OOZOX مؤمنة بمعايير حماية بنكية صارمة:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                listOf(
                    "🛡️ حماية 100% ضد حقن قواعد البيانات (Room Compile-time Bindings & Type Safety).",
                    "🧼 تطهير مدخلات المستخدم ومنع إدراج أكواد خبيثة أو Control Sequences.",
                    "🔒 تشفير ثنائي AES-256-GCM لكافة عمليات النقل بين الهواتف والمزامنة.",
                    "⏱️ نظام Rate Limiter صارم لمنع محاولات كسر كلمات المرور ورموز OTP.",
                    "💼 عزل تام لحسابات وخزائن الشركات الفرعية مع حماية PIN المستقلة."
                ).forEach { item ->
                    Surface(
                        color = OozoxTeal.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, OozoxTeal.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = item,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(8.dp),
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = OozoxTeal, contentColor = Color.Black)
            ) {
                Text("إغلاق", fontWeight = FontWeight.Bold)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
internal fun ProfileTrustedDevicesAndPasswordDialog(
    trustedDevices: List<com.example.util.ApiTrustedDeviceDto>,
    isLoadingDevices: Boolean,
    onRefreshDevices: () -> Unit,
    onRevokeDevice: (deviceId: String, onDone: (Boolean, String) -> Unit) -> Unit,
    onChangePassword: suspend (currentPassword: String, newPassword: String, stepUpChallengeId: String?, stepUpCode: String?) -> com.example.data.repository.PasswordChangeOutcome,
    onRequestStepUpOtp: suspend (channel: String) -> com.example.data.repository.OtpRequestOutcome,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val appToast = LocalAppToast.current
    val scope = rememberCoroutineScope()
    val currentDeviceId = remember { com.example.util.SecureCredentialVault.getOrCreateDeviceId(context) }

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Trusted Devices, 1 = Change Password

    // Change Password state
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var needsStepUpOtp by remember { mutableStateOf(false) }
    var stepUpChallengeId by remember { mutableStateOf<String?>(null) }
    var stepUpOtpCode by remember { mutableStateOf("") }
    var selectedChannel by remember { mutableStateOf(com.example.ui.components.OtpDeliveryChannel.TELEGRAM) }
    var isSubmitting by remember { mutableStateOf(false) }
    var formError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        onRefreshDevices()
    }

    fun runWithSensitiveBiometric(onVerified: () -> Unit) {
        if (activity != null && BiometricHelper.checkBiometricStatus(context) == BiometricStatus.AVAILABLE) {
            BiometricHelper.authenticate(
                activity = activity,
                title = "تأكيد العملية الحساسة",
                subtitle = "إدارة الأجهزة الموثوقة وكلمة المرور",
                description = "يلزم تأكيد البصمة لإتمام هذا الإجراء الأمني",
                negativeButtonText = "إلغاء",
                onSuccess = { onVerified() },
                onError = { _, err -> formError = err.toString() },
                onFailed = { formError = "تعذر التحقق من البصمة" }
            )
        } else {
            onVerified()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .widthIn(max = 520.dp)
                .testTag("trusted_devices_password_dialog"),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.35f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.DevicesOther, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("الأجهزة الموثوقة وكلمة المرور", fontSize = 15.5.sp, fontWeight = FontWeight.Bold)
                            Text("ثقة السيرفر (30 يوماً) والتحكم بالجلسات", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    contentColor = GoldPrimary,
                    modifier = Modifier.clip(RoundedCornerShape(10.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0; formError = null },
                        text = { Text("الأجهزة (${trustedDevices.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1; formError = null },
                        text = { Text("تغيير كلمة المرور", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                }

                if (selectedTab == 0) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 380.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "تنتهي ثقة كل جهاز بعد 30 يوماً أو عند تغيير كلمة المرور.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = onRefreshDevices, modifier = Modifier.heightIn(min = 48.dp)) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("تحديث", fontSize = 11.5.sp)
                            }
                        }

                        if (isLoadingDevices) {
                            Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = GoldPrimary)
                            }
                        } else if (trustedDevices.isEmpty()) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.Default.PhonelinkErase, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(30.dp))
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("لا توجد أجهزة موثوقة مسجلة على السيرفر حالياً.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        } else {
                            trustedDevices.forEach { device ->
                                val isThisDevice = device.deviceId == currentDeviceId
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    border = BorderStroke(1.dp, if (isThisDevice) GoldPrimary.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = device.deviceName.ifBlank { device.deviceId },
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                if (isThisDevice) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Surface(color = GoldPrimary.copy(alpha = 0.18f), shape = RoundedCornerShape(6.dp)) {
                                                        Text("هذا الجهاز", fontSize = 10.sp, color = GoldPrimary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                                    }
                                                }
                                            }
                                            Text("المنصة: ${device.platform} • صالح حتى: ${device.trustedUntil.take(10)}", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                runWithSensitiveBiometric {
                                                    onRevokeDevice(device.deviceId) { ok, msg ->
                                                        appToast.showToast(msg, if (ok) AppToastType.SUCCESS else AppToastType.ERROR)
                                                        if (ok && isThisDevice) onDismiss()
                                                    }
                                                }
                                            },
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.heightIn(min = 48.dp)
                                        ) {
                                            Text("إلغاء الثقة", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            color = StatusOrange.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, StatusOrange.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "تنبيه: تغيير كلمة المرور يُلغي تلقائياً جميع الأجهزة الموثوقة (30 يوماً) ويتطلب التحقق بالبصمة، أو رمز OTP إذا كانت الجلسة قديمة.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(10.dp)
                            )
                        }

                        OutlinedTextField(
                            value = currentPassword,
                            onValueChange = { currentPassword = it; formError = null },
                            label = { Text("كلمة المرور الحالية") },
                            singleLine = true,
                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("input_current_password")
                        )

                        OutlinedTextField(
                            value = newPassword,
                            onValueChange = { newPassword = it; formError = null },
                            label = { Text("كلمة المرور الجديدة (8 أحرف فأكثر)") },
                            singleLine = true,
                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("input_new_password")
                        )

                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it; formError = null },
                            label = { Text("تأكيد كلمة المرور الجديدة") },
                            singleLine = true,
                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("input_confirm_new_password")
                        )

                        if (needsStepUpOtp) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                com.example.ui.components.OtpDeliveryChannel.entries.forEach { ch ->
                                    FilterChip(
                                        selected = selectedChannel == ch,
                                        onClick = { selectedChannel = ch },
                                        label = { Text(ch.titleAr, fontSize = 10.5.sp) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = stepUpOtpCode,
                                    onValueChange = { stepUpOtpCode = it.filter { c -> c.isDigit() }.take(6); formError = null },
                                    label = { Text("رمز OTP") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f).testTag("input_step_up_otp")
                                )
                                OutlinedButton(
                                    onClick = {
                                        scope.launch {
                                            isSubmitting = true
                                            when (val otpRes = onRequestStepUpOtp(selectedChannel.name)) {
                                                is com.example.data.repository.OtpRequestOutcome.Sent -> {
                                                    stepUpChallengeId = otpRes.data.challengeId
                                                    appToast.showToast(otpRes.message, AppToastType.INFO)
                                                }
                                                is com.example.data.repository.OtpRequestOutcome.Error -> {
                                                    formError = otpRes.message
                                                }
                                            }
                                            isSubmitting = false
                                        }
                                    },
                                    enabled = !isSubmitting,
                                    modifier = Modifier.heightIn(min = 48.dp)
                                ) {
                                    Text(if (stepUpChallengeId == null) "إرسال OTP" else "إعادة إرسال", fontSize = 11.sp)
                                }
                            }
                        }

                        if (!formError.isNullOrBlank()) {
                            Text(formError!!, color = MaterialTheme.colorScheme.error, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                if (currentPassword.isBlank() || newPassword.length < 8) {
                                    formError = "أدخل كلمة المرور الحالية وكلمة مرور جديدة لا تقل عن 8 أحرف."
                                    return@Button
                                }
                                if (newPassword != confirmPassword) {
                                    formError = "كلمتا المرور غير متطابقتين."
                                    return@Button
                                }
                                if (needsStepUpOtp && (stepUpChallengeId.isNullOrBlank() || stepUpOtpCode.length != 6)) {
                                    formError = "أرسل وأدخل رمز التحقق OTP المكوّن من 6 أرقام."
                                    return@Button
                                }
                                runWithSensitiveBiometric {
                                    scope.launch {
                                        isSubmitting = true
                                        formError = null
                                        when (val out = onChangePassword(currentPassword, newPassword, stepUpChallengeId, stepUpOtpCode.takeIf { it.length == 6 })) {
                                            is com.example.data.repository.PasswordChangeOutcome.Changed -> {
                                                isSubmitting = false
                                                appToast.showToast(out.message, AppToastType.SUCCESS)
                                                onRefreshDevices()
                                                onDismiss()
                                            }
                                            is com.example.data.repository.PasswordChangeOutcome.StepUpRequired -> {
                                                isSubmitting = false
                                                needsStepUpOtp = true
                                                formError = out.message
                                                // Auto-request step-up OTP right away for smoother UX
                                                when (val otpRes = onRequestStepUpOtp(selectedChannel.name)) {
                                                    is com.example.data.repository.OtpRequestOutcome.Sent -> {
                                                        stepUpChallengeId = otpRes.data.challengeId
                                                        appToast.showToast(otpRes.message, AppToastType.INFO)
                                                    }
                                                    is com.example.data.repository.OtpRequestOutcome.Error -> {
                                                        formError = otpRes.message
                                                    }
                                                }
                                            }
                                            is com.example.data.repository.PasswordChangeOutcome.Error -> {
                                                isSubmitting = false
                                                formError = out.message
                                            }
                                        }
                                    }
                                }
                            },
                            enabled = !isSubmitting,
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = MaterialTheme.colorScheme.background),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("btn_submit_change_password")
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.background)
                            } else {
                                Text("تحديث كلمة المرور", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
