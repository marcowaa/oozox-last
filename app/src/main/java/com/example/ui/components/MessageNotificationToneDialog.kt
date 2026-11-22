package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.adaptiveGoldPrimary
import com.example.ui.theme.adaptiveOnPrimary
import com.example.ui.theme.GoldPrimary
import com.example.util.LocalAppLanguage
import com.example.util.MessageNotificationSoundHelper
import com.example.util.MessageNotificationTone
import com.example.util.Translations
import com.example.util.tr
import com.example.util.trFmt

@Composable
fun MessageNotificationToneDialog(
    initialToneId: String? = null,
    onDismiss: () -> Unit,
    onToneSelected: (toneId: String) -> Unit = {}
) {
    val context = LocalContext.current
    val currentLang = LocalAppLanguage.current

    var selectedToneId by remember {
        mutableStateOf(initialToneId ?: MessageNotificationSoundHelper.getSelectedTone(context))
    }
    var isSoundEnabled by remember {
        mutableStateOf(MessageNotificationSoundHelper.isSoundEnabled(context))
    }
    var isVibrationEnabled by remember {
        mutableStateOf(MessageNotificationSoundHelper.isVibrationEnabled(context))
    }

    val playingToneId by MessageNotificationSoundHelper.currentPlayingToneId.collectAsStateWithLifecycle()

    DisposableEffect(Unit) {
        onDispose {
            MessageNotificationSoundHelper.stopPlayback()
        }
    }

    Dialog(
        onDismissRequest = {
            MessageNotificationSoundHelper.stopPlayback()
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        CompositionLocalProvider(
            LocalLayoutDirection provides currentLang.direction,
            LocalAppLanguage provides currentLang
        ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.2.dp, GoldPrimary.copy(alpha = 0.5f)),
            shadowElevation = 18.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header with Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = GoldPrimary.copy(alpha = 0.15f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = GoldPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Column {
                            Text(
                                text = "${tr("tone.dialog_title")} 🔔",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = tr("tone.dialog_sub"),
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            MessageNotificationSoundHelper.stopPlayback()
                            onDismiss()
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = tr("common.close"),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // Sound & Vibration Toggles Card
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Sound switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = null,
                                    tint = if (isSoundEnabled) GoldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Column {
                                    Text(
                                        text = tr("tone.sound_title"),
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (isSoundEnabled) tr("tone.sound_on") else tr("tone.sound_off"),
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Switch(
                                checked = isSoundEnabled,
                                onCheckedChange = {
                                    isSoundEnabled = it
                                    MessageNotificationSoundHelper.setSoundEnabled(context, it)
                                    if (!it) {
                                        MessageNotificationSoundHelper.stopPlayback()
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = GoldPrimary
                                )
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                        // Vibration switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Vibration,
                                    contentDescription = null,
                                    tint = if (isVibrationEnabled) GoldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Column {
                                    Text(
                                        text = tr("tone.vib_title"),
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (isVibrationEnabled) tr("tone.vib_on") else tr("tone.vib_off"),
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Switch(
                                checked = isVibrationEnabled,
                                onCheckedChange = {
                                    isVibrationEnabled = it
                                    MessageNotificationSoundHelper.setVibrationEnabled(context, it)
                                    if (it) {
                                        MessageNotificationSoundHelper.triggerMessageVibration(context)
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = GoldPrimary
                                )
                            )
                        }
                    }
                }

                // Tone List Section Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tr("tone.choose_fav"),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = GoldPrimary
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = GoldPrimary.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = trFmt("tone.count_fmt", "count" to MessageNotificationSoundHelper.AVAILABLE_TONES.size),
                            fontSize = 10.5.sp,
                            color = GoldPrimary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }

                // List of built-in tones
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(290.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = MessageNotificationSoundHelper.AVAILABLE_TONES,
                        key = { it.id }
                    ) { tone ->
                        NotificationToneItemRow(
                            tone = tone,
                            isSelected = selectedToneId == tone.id,
                            isPlaying = playingToneId == tone.id,
                            isSoundEnabled = isSoundEnabled,
                            onSelect = {
                                selectedToneId = tone.id
                                MessageNotificationSoundHelper.setSelectedTone(context, tone.id)
                                onToneSelected(tone.id)
                                // Automatically preview when tapped
                                MessageNotificationSoundHelper.playTonePreview(context, tone.id)
                            },
                            onTogglePlay = {
                                if (playingToneId == tone.id) {
                                    MessageNotificationSoundHelper.stopPlayback()
                                } else {
                                    selectedToneId = tone.id
                                    MessageNotificationSoundHelper.setSelectedTone(context, tone.id)
                                    onToneSelected(tone.id)
                                    MessageNotificationSoundHelper.playTonePreview(context, tone.id)
                                }
                            }
                        )
                    }
                }

                // Bottom Actions: Test alert & Save / Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            MessageNotificationSoundHelper.setSelectedTone(context, selectedToneId)
                            MessageNotificationSoundHelper.playNewMessageSound(context)
                            val toneName = MessageNotificationSoundHelper.getToneById(selectedToneId).localizedName(currentLang)
                            Toast.makeText(
                                context,
                                Translations.format("tone.test_toast_fmt", currentLang, "name" to toneName),
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.6f)),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text(
                            text = tr("tone.test_btn"),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldPrimary
                        )
                    }

                    Button(
                        onClick = {
                            MessageNotificationSoundHelper.setSelectedTone(context, selectedToneId)
                            MessageNotificationSoundHelper.stopPlayback()
                            onToneSelected(selectedToneId)
                            Toast.makeText(context, Translations.get("tone.saved_toast", currentLang), Toast.LENGTH_SHORT).show()
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = adaptiveGoldPrimary,
                            contentColor = adaptiveOnPrimary
                        )
                    ) {
                        Text(
                            text = tr("tone.save_btn"),
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

@Composable
private fun NotificationToneItemRow(
    tone: MessageNotificationTone,
    isSelected: Boolean,
    isPlaying: Boolean,
    isSoundEnabled: Boolean,
    onSelect: () -> Unit,
    onTogglePlay: () -> Unit
) {
    val currentLang = LocalAppLanguage.current
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) GoldPrimary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
        label = "toneBorderColor"
    )
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) GoldPrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        label = "toneBgColor"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isPlaying) 1.15f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = bgColor,
        border = BorderStroke(if (isSelected) 1.5.dp else 0.8.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Radio button indicator
                RadioButton(
                    selected = isSelected,
                    onClick = { onSelect() },
                    colors = RadioButtonDefaults.colors(
                        selectedColor = GoldPrimary,
                        unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.size(24.dp)
                )

                // Tone Emoji / Icon
                Surface(
                    shape = CircleShape,
                    color = if (isSelected) GoldPrimary.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .size(34.dp)
                        .scale(if (isPlaying) pulseScale else 1f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = tone.icon,
                            fontSize = 17.sp
                        )
                    }
                }

                // Tone Name and Details
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = tone.localizedName(currentLang),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = if (isSelected) GoldPrimary else MaterialTheme.colorScheme.onSurface
                        )

                        if (isSelected) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = GoldPrimary.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = tr("tone.current_badge"),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldPrimary,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = tone.localizedDescription(currentLang),
                        fontSize = 10.5.sp,
                        lineHeight = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Play / Stop preview button
            FilledTonalIconButton(
                onClick = onTogglePlay,
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = if (isPlaying) adaptiveGoldPrimary else adaptiveGoldPrimary.copy(alpha = 0.15f),
                    contentColor = if (isPlaying) adaptiveOnPrimary else adaptiveGoldPrimary
                )
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) tr("tone.stop_cd") else tr("tone.play_cd"),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
