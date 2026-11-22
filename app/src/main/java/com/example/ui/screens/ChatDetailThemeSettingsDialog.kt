package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.ChatEntity
import com.example.ui.components.MessageNotificationToneDialog
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.adaptiveGoldPrimary
import com.example.ui.theme.adaptiveOnPrimary
import com.example.util.BlockManager
import com.example.util.MessageNotificationSoundHelper
import com.example.util.TranslationStyleHelper

@Composable
internal fun ChatDetailThemeSettingsDialog(
    chat: ChatEntity,
    customChatBg: String,
    customChatBgColor: Color?,
    customBubbleStyle: String,
    customBubbleColor: Color?,
    customFontColorStyle: String,
    customFontColor: Color?,
    translationFontSize: String,
    translationColorStyle: String,
    isUserBlocked: Boolean,
    onDismiss: () -> Unit,
    onBlockedChange: (Boolean) -> Unit,
    onSaveTheme: (
        bgStyle: String,
        bgColor: Color?,
        bubbleStyle: String,
        bubbleColor: Color?,
        fontStyle: String,
        fontColor: Color?,
        trFontSize: String,
        trColorStyle: String
    ) -> Unit
) {
    val context = LocalContext.current

    var draftChatBgStyle by remember { mutableStateOf(customChatBg) }
    var draftChatBgColor by remember { mutableStateOf(customChatBgColor) }

    var draftBubbleStyle by remember { mutableStateOf(customBubbleStyle) }
    var draftBubbleColor by remember { mutableStateOf(customBubbleColor) }

    var draftFontColorStyle by remember { mutableStateOf(customFontColorStyle) }
    var draftFontColor by remember { mutableStateOf(customFontColor) }

    var draftTranslationFontSize by remember { mutableStateOf(translationFontSize) }
    var draftTranslationColorStyle by remember { mutableStateOf(translationColorStyle) }

    var showNotificationTonePickerInChat by remember { mutableStateOf(false) }
    var colorPickerTarget by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 540.dp)
                    .heightIn(max = 680.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.4f)),
                shadowElevation = 16.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Palette, contentDescription = null, tint = GoldPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "تخصيص ألوان المحادثة",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.5.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            TextButton(
                                onClick = {
                                    draftChatBgStyle = "default"
                                    draftChatBgColor = null
                                    draftBubbleStyle = "auto"
                                    draftBubbleColor = null
                                    draftFontColorStyle = "auto"
                                    draftFontColor = null
                                    draftTranslationFontSize = "medium"
                                    draftTranslationColorStyle = "gold"
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .heightIn(min = 36.dp)
                                    .testTag("chat_theme_reset_btn")
                            ) {
                                Text("إعادة ضبط", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = GoldPrimary)
                            }
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    HorizontalDivider()

                    ChatThemeLivePreviewCard(
                        draftChatBgStyle = draftChatBgStyle,
                        draftChatBgColor = draftChatBgColor,
                        draftBubbleStyle = draftBubbleStyle,
                        draftBubbleColor = draftBubbleColor,
                        draftFontColorStyle = draftFontColorStyle,
                        draftFontColor = draftFontColor,
                        draftTranslationColorStyle = draftTranslationColorStyle,
                        draftTranslationFontSize = draftTranslationFontSize
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .testTag("chat_theme_options_scroll"),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        ChatThemePresetAndSwatchSection(
                            title = "1. خلفية المحادثة ️",
                            swatchPrompt = "أو اختر لونك المفضل مباشرة (Color Picker):",
                            presets = listOf(
                                "default" to "الافتراضي",
                                "gold" to "ذهبي",
                                "emerald" to "زمردي",
                                "navy" to "أزرق ليلي",
                                "light" to "️ فاتح"
                            ),
                            swatches = listOf(
                                Color(0xFF0B0F19), Color(0xFF1E1B10), Color(0xFF062C22), Color(0xFF0F172A),
                                Color(0xFF18181B), Color(0xFF2E1065), Color(0xFF4C0519), Color(0xFF3B1254),
                                Color(0xFF064E3B), Color(0xFF0F766E), Color(0xFF451A03), Color(0xFFF1F5F9)
                            ),
                            selectedPreset = draftChatBgStyle,
                            selectedColor = draftChatBgColor,
                            onSelectPreset = {
                                draftChatBgStyle = it
                                draftChatBgColor = null
                            },
                            onSelectColor = { draftChatBgColor = it },
                            onOpenCustomPicker = { colorPickerTarget = "bg" },
                            lightCheckColor = Color.Black,
                            darkCheckColor = GoldPrimary
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        ChatThemePresetAndSwatchSection(
                            title = "2. لون فقاعات الرسائل",
                            swatchPrompt = "أو اختر لون فقاعات الرسائل:",
                            presets = listOf(
                                "auto" to "تلقائي",
                                "gold_dark" to "ذهبي داكن",
                                "slate" to "رمادي داكن",
                                "emerald_glass" to "زمردي",
                                "violet" to "بنفسجي"
                            ),
                            swatches = listOf(
                                GoldDark, Color(0xFF1E293B), Color(0xFF064E3B), Color(0xFF2E1065),
                                Color(0xFF831843), Color(0xFF1E3A8A), Color(0xFF78350F), Color(0xFF334155),
                                Color(0xFF0F766E), Color(0xFF4C0519), Color(0xFF1C1917), Color(0xFFF3F4F6)
                            ),
                            selectedPreset = draftBubbleStyle,
                            selectedColor = draftBubbleColor,
                            onSelectPreset = {
                                draftBubbleStyle = it
                                draftBubbleColor = null
                            },
                            onSelectColor = { draftBubbleColor = it },
                            onOpenCustomPicker = { colorPickerTarget = "bubble" },
                            lightCheckColor = Color.Black,
                            darkCheckColor = GoldPrimary
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        ChatThemePresetAndSwatchSection(
                            title = "3. لون خط النصوص ️",
                            swatchPrompt = "أو اختر لون خط القراءة:",
                            presets = listOf(
                                "auto" to "تلقائي",
                                "gold" to "ذهبي",
                                "white" to "أبيض",
                                "emerald" to "أخضر",
                                "platinum" to "️ بلاتيني"
                            ),
                            swatches = listOf(
                                GoldLight, Color.White, Color(0xFF10B981), Color(0xFFE2E8F0),
                                Color(0xFF38BDF8), Color(0xFFF43F5E), Color(0xFFF59E0B), Color(0xFFA855F7),
                                Color(0xFF84CC16), Color(0xFFFACC15), Color(0xFF000000)
                            ),
                            selectedPreset = draftFontColorStyle,
                            selectedColor = draftFontColor,
                            onSelectPreset = {
                                draftFontColorStyle = it
                                draftFontColor = null
                            },
                            onSelectColor = { draftFontColor = it },
                            onOpenCustomPicker = { colorPickerTarget = "font" },
                            lightCheckColor = Color.Black,
                            darkCheckColor = Color.White
                        )

                        HorizontalDivider()

                        // Block User Section
                        Surface(
                            color = if (isUserBlocked) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, if (isUserBlocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
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
                                        text = "حظر هذا المستخدم",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp,
                                        color = if (isUserBlocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (isUserBlocked) "المستخدم محظور حالياً (لا تظهر له أي علامة حظر لحفظ اللباقة)" else "حظر سري: لن تظهر له أي علامة حظر، وتُحجب تحديثات ملفك وصورتك عنه تلقائياً",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = isUserBlocked,
                                    onCheckedChange = { blocked ->
                                        onBlockedChange(blocked)
                                        val targetId = chat.contactId
                                        if (blocked) {
                                            BlockManager.blockUser(context, targetId)
                                            Toast.makeText(context, "تم تفعيل الحظر وحجب بياناتك وتحديثاتك", Toast.LENGTH_SHORT).show()
                                        } else {
                                            BlockManager.unblockUser(context, targetId)
                                            Toast.makeText(context, "تم إلغاء الحظر", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = MaterialTheme.colorScheme.error
                                    )
                                )
                            }
                        }

                        ChatTranslationAndSoundSection(
                            draftTranslationColorStyle = draftTranslationColorStyle,
                            draftTranslationFontSize = draftTranslationFontSize,
                            showNotificationTonePickerInChat = showNotificationTonePickerInChat,
                            onSelectTranslationColor = { draftTranslationColorStyle = it },
                            onSelectTranslationSize = { draftTranslationFontSize = it },
                            onOpenTonePicker = { showNotificationTonePickerInChat = true }
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("chat_theme_cancel_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("إلغاء", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = {
                                onSaveTheme(
                                    draftChatBgStyle,
                                    draftChatBgColor,
                                    draftBubbleStyle,
                                    draftBubbleColor,
                                    draftFontColorStyle,
                                    draftFontColor,
                                    draftTranslationFontSize,
                                    draftTranslationColorStyle
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = adaptiveGoldPrimary, contentColor = adaptiveOnPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("chat_theme_save_btn")
                        ) {
                            Text("حفظ", color = adaptiveOnPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }

    if (showNotificationTonePickerInChat) {
        MessageNotificationToneDialog(
            onDismiss = { showNotificationTonePickerInChat = false }
        )
    }

    colorPickerTarget?.let { target ->
        ChatThemeCustomColorPickerDialog(
            target = target,
            onDismiss = { colorPickerTarget = null },
            onApplyColor = { c ->
                when (target) {
                    "bg" -> draftChatBgColor = c
                    "bubble" -> draftBubbleColor = c
                    "font" -> draftFontColor = c
                }
                colorPickerTarget = null
            }
        )
    }
}

@Composable
private fun ChatThemeLivePreviewCard(
    draftChatBgStyle: String,
    draftChatBgColor: Color?,
    draftBubbleStyle: String,
    draftBubbleColor: Color?,
    draftFontColorStyle: String,
    draftFontColor: Color?,
    draftTranslationColorStyle: String,
    draftTranslationFontSize: String
) {
    val effectivePreviewBg = when {
        draftChatBgColor != null -> draftChatBgColor
        draftChatBgStyle == "gold" -> Color(0xFF1E1B10)
        draftChatBgStyle == "emerald" -> Color(0xFF062C22)
        draftChatBgStyle == "navy" -> Color(0xFF0F172A)
        draftChatBgStyle == "light" -> Color(0xFFF1F5F9)
        else -> Color(0xFF0B0F19)
    }
    val effectivePreviewBubble = when {
        draftBubbleColor != null -> draftBubbleColor
        draftBubbleStyle == "gold_dark" -> GoldDark
        draftBubbleStyle == "slate" -> Color(0xFF1E293B)
        draftBubbleStyle == "emerald_glass" -> Color(0xFF064E3B)
        draftBubbleStyle == "violet" -> Color(0xFF2E1065)
        else -> MaterialTheme.colorScheme.primaryContainer
    }
    val effectivePreviewFont = when {
        draftFontColor != null -> draftFontColor
        draftFontColorStyle == "gold" -> GoldLight
        draftFontColorStyle == "white" -> Color.White
        draftFontColorStyle == "emerald" -> Color(0xFF10B981)
        draftFontColorStyle == "platinum" -> Color(0xFFE2E8F0)
        else -> MaterialTheme.colorScheme.onPrimaryContainer
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("chat_theme_live_preview"),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.5f)),
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(effectivePreviewBg)
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "معاينة شاشة المحادثة المباشرة",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = effectivePreviewFont.copy(alpha = 0.9f)
                )
                Surface(
                    color = GoldPrimary.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "تحديث فوري",
                        fontSize = 10.sp,
                        color = GoldPrimary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp, bottomEnd = 14.dp, bottomStart = 2.dp),
                color = effectivePreviewBubble,
                border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth(0.85f)
            ) {
                Text(
                    text = "مرحباً بك! هذه معاينة مباشرة لألوان خلفية الشاشة، الرسائل، ولون النص",
                    color = effectivePreviewFont,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(8.dp)
                )
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFF1B2028) else Color(0xFFF1F5F9),
                border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .padding(start = 6.dp)
            ) {
                Column(modifier = Modifier.padding(7.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(11.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ترجمة سياقية (معاينة التباين)", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = GoldPrimary)
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "مرحباً! هذه معاينة حية لخط ولون الترجمة المتباين والمريح للعين.",
                        color = TranslationStyleHelper.resolveColor(draftTranslationColorStyle, MaterialTheme.colorScheme.background.luminance() < 0.5f),
                        fontSize = TranslationStyleHelper.resolveFontSize(draftTranslationFontSize).sp,
                        lineHeight = TranslationStyleHelper.resolveLineHeight(draftTranslationFontSize).sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp, bottomEnd = 2.dp, bottomStart = 14.dp),
                color = effectivePreviewBubble.copy(alpha = 0.95f),
                border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.3f)),
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .align(Alignment.End)
            ) {
                Text(
                    text = "ممتاز، التناسق والوضوح رائع جداً! اضغط 'حفظ' لتطبيق التعديلات",
                    color = effectivePreviewFont,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}

@Composable
private fun ChatThemePresetAndSwatchSection(
    title: String,
    swatchPrompt: String,
    presets: List<Pair<String, String>>,
    swatches: List<Color>,
    selectedPreset: String,
    selectedColor: Color?,
    onSelectPreset: (String) -> Unit,
    onSelectColor: (Color) -> Unit,
    onOpenCustomPicker: () -> Unit,
    lightCheckColor: Color,
    darkCheckColor: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = GoldPrimary)
            OutlinedButton(
                onClick = onOpenCustomPicker,
                modifier = Modifier.height(30.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.5f))
            ) {
                Text("+ لون خاص", fontSize = 11.sp, color = GoldPrimary)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            presets.forEach { (key, label) ->
                FilterChip(
                    selected = selectedPreset == key && selectedColor == null,
                    onClick = { onSelectPreset(key) },
                    label = { Text(label, fontSize = 11.5.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = adaptiveGoldPrimary,
                        selectedLabelColor = adaptiveOnPrimary
                    )
                )
            }
        }

        Text(swatchPrompt, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            swatches.forEach { colorSwatch ->
                val isSelected = selectedColor == colorSwatch
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(colorSwatch)
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) GoldPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            shape = CircleShape
                        )
                        .clickable { onSelectColor(colorSwatch) },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = if (colorSwatch.luminance() > 0.5f) lightCheckColor else darkCheckColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatTranslationAndSoundSection(
    draftTranslationColorStyle: String,
    draftTranslationFontSize: String,
    showNotificationTonePickerInChat: Boolean,
    onSelectTranslationColor: (String) -> Unit,
    onSelectTranslationSize: (String) -> Unit,
    onOpenTonePicker: () -> Unit
) {
    val context = LocalContext.current

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("تخصيص خط ولون الترجمة 🌐", fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = GoldPrimary)
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = GoldPrimary.copy(alpha = 0.15f)
            ) {
                Text(
                    text = "تباين عالي وراحة للعين",
                    fontSize = 10.sp,
                    color = GoldPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        Text("لون خط الترجمة:", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            TranslationStyleHelper.COLOR_OPTIONS.forEach { opt ->
                val isSelected = draftTranslationColorStyle == opt.id
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) GoldPrimary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(if (isSelected) 1.5.dp else 0.5.dp, if (isSelected) GoldPrimary else MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.clickable { onSelectTranslationColor(opt.id) }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(opt.previewColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = opt.titleAr,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) GoldPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        Text("حجم خط الترجمة:", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            TranslationStyleHelper.SIZE_OPTIONS.forEach { sz ->
                val isSelected = draftTranslationFontSize == sz.id
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) GoldPrimary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(if (isSelected) 1.5.dp else 0.5.dp, if (isSelected) GoldPrimary else MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.clickable { onSelectTranslationSize(sz.id) }
                ) {
                    Text(
                        text = sz.titleAr,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) GoldPrimary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        val chatToneId = remember(showNotificationTonePickerInChat) {
            MessageNotificationSoundHelper.getSelectedTone(context)
        }
        val chatToneObj = remember(chatToneId) {
            MessageNotificationSoundHelper.getToneById(chatToneId)
        }
        val isChatSoundOn = remember(showNotificationTonePickerInChat) {
            MessageNotificationSoundHelper.isSoundEnabled(context)
        }

        Text("نغمة تنبيه الرسائل الجديدة:", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = BorderStroke(0.8.dp, GoldPrimary.copy(alpha = 0.4f)),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenTonePicker)
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(chatToneObj.icon, fontSize = 18.sp)
                    Column {
                        Text(
                            text = if (isChatSoundOn) chatToneObj.nameAr else "صامت (التنبيه الصوتي معطل)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isChatSoundOn) GoldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "انقر للتغيير أو الاستماع للنغمات المدمجة",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = GoldPrimary.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "تغيير 🎵",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatThemeCustomColorPickerDialog(
    target: String,
    onDismiss: () -> Unit,
    onApplyColor: (Color) -> Unit
) {
    var customHexInput by remember { mutableStateOf("") }
    val targetTitle = when (target) {
        "bg" -> "خلفية الشاشة"
        "bubble" -> "فقاعات الرسائل"
        else -> "لون الخط"
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.5f)),
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "اختيار لون خاص لـ ($targetTitle)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                Text("اختر لوناً من شبكة الألوان الخاصة:", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                val extendedColors = listOf(
                    Color(0xFFFFD700), Color(0xFFD4AF37), Color(0xFF10B981), Color(0xFF0F766E),
                    Color(0xFF38BDF8), Color(0xFF1E3A8A), Color(0xFF8B5CF6), Color(0xFF4C1D95),
                    Color(0xFFEC4899), Color(0xFFEF4444), Color(0xFFFFFFFF), Color(0xFFE2E8F0),
                    Color(0xFF1E293B), Color(0xFF0F172A), Color(0xFF18181B), Color(0xFF000000)
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    extendedColors.chunked(4).forEach { rowColors ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            rowColors.forEach { c ->
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(c)
                                        .border(1.dp, GoldPrimary.copy(alpha = 0.4f), CircleShape)
                                        .clickable { onApplyColor(c) }
                                )
                            }
                        }
                    }
                }

                HorizontalDivider()

                Text("أو أدخل كود اللون (Hex Code):", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = customHexInput,
                        onValueChange = { customHexInput = it },
                        placeholder = { Text("#FFD700", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    Button(
                        onClick = {
                            val parsed = parseHexColor(customHexInput)
                            if (parsed != null) {
                                onApplyColor(parsed)
                                customHexInput = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = adaptiveGoldPrimary, contentColor = adaptiveOnPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text("تطبيق", color = adaptiveOnPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
