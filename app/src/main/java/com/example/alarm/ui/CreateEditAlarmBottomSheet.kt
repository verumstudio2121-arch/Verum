package com.example.alarm.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.CustomRingtone
import com.example.audio.CustomRingtoneManager
import com.example.audio.SoundPlayer
import com.example.data.AlarmModel
import com.example.ui.theme.AccentRed
import com.example.ui.theme.DeepBlack
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassBorderBright
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.GlassSurfaceElevated
import com.example.ui.theme.LiquidGlassButton
import com.example.ui.theme.LiquidGlassCard
import com.example.ui.theme.LocalAccentColor
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.util.LocalHapticManager

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateEditAlarmBottomSheet(
    initialAlarm: AlarmModel? = null,
    is24Hour: Boolean = false,
    onSave: (AlarmModel) -> Unit,
    onDelete: ((String) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val haptic = LocalHapticManager.current
    val accentColor = LocalAccentColor.current.primary
    val context = LocalContext.current
    val ringtoneManager = remember { CustomRingtoneManager(context) }

    // Initial state
    val isEditing = initialAlarm != null
    val initHour = initialAlarm?.hour ?: 7
    val initMinute = initialAlarm?.minute ?: 0
    val initIsPm = initHour >= 12
    val initDisplayHour = when {
        initHour == 0 -> 12
        initHour > 12 -> initHour - 12
        else -> initHour
    }

    var displayHour by remember { mutableIntStateOf(initDisplayHour) }
    var selectedMinute by remember { mutableIntStateOf(initMinute) }
    var isPm by remember { mutableStateOf(initIsPm) }
    var label by remember { mutableStateOf(initialAlarm?.label ?: "Wake Up") }
    var repeatType by remember { mutableStateOf(initialAlarm?.repeatType ?: "Once") }
    var customDays by remember { mutableStateOf(initialAlarm?.customDays ?: emptySet()) }
    var selectedSound by remember { mutableStateOf(initialAlarm?.soundName ?: "Gentle") }
    var vibrate by remember { mutableStateOf(initialAlarm?.vibrate ?: true) }
    var snoozeEnabled by remember { mutableStateOf(initialAlarm?.snoozeEnabled ?: true) }

    var isPreviewPlaying by remember { mutableStateOf(false) }

    // Custom ringtones list
    var customRingtones by remember { mutableStateOf(ringtoneManager.getCustomRingtones()) }

    // Audio file picker launcher
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val imported = ringtoneManager.importRingtoneFromUri(uri)
            if (imported != null) {
                customRingtones = ringtoneManager.getCustomRingtones()
                selectedSound = imported.name
                SoundPlayer.previewSound(context, imported.name)
                isPreviewPlaying = true
                haptic.confirm()
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            SoundPlayer.stopPreview()
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            SoundPlayer.stopPreview()
            onDismiss()
        },
        sheetState = sheetState,
        containerColor = Color(0xFF101117),
        contentColor = Color.White,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(44.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.25f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
        ) {
            // Header: Cancel, Title, Done (Matching reference video)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Cancel
                Text(
                    text = "Cancel",
                    color = TextSecondary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            haptic.buttonClick()
                            SoundPlayer.stopPreview()
                            onDismiss()
                        }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                )

                Text(
                    text = if (isEditing) "Edit alarm" else "New alarm",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )

                // Done
                Text(
                    text = "Done",
                    color = accentColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            SoundPlayer.stopPreview()
                            val actualHour = when {
                                !isPm && displayHour == 12 -> 0
                                !isPm -> displayHour
                                isPm && displayHour == 12 -> 12
                                else -> displayHour + 12
                            }

                            val finalAlarm = initialAlarm?.copy(
                                hour = actualHour,
                                minute = selectedMinute,
                                label = label.trim().ifEmpty { "Alarm" },
                                repeatType = repeatType,
                                customDays = customDays,
                                soundName = selectedSound,
                                vibrate = vibrate,
                                snoozeEnabled = snoozeEnabled
                            ) ?: AlarmModel(
                                hour = actualHour,
                                minute = selectedMinute,
                                label = label.trim().ifEmpty { "Alarm" },
                                isEnabled = true,
                                repeatType = repeatType,
                                customDays = customDays,
                                soundName = selectedSound,
                                vibrate = vibrate,
                                snoozeEnabled = snoozeEnabled
                            )

                            haptic.confirm()
                            onSave(finalAlarm)
                            onDismiss()
                        }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                )
            }

            // Dual-mode Scroller and Direct Type Alarm Time Picker
            AlarmTimePickerScroller(
                displayHour = displayHour,
                minute = selectedMinute,
                isPm = isPm,
                is24Hour = is24Hour,
                onHourChange = { displayHour = it },
                onMinuteChange = { selectedMinute = it },
                onAmPmChange = { isPm = it },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Label Input
            Text("Label", color = TextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                singleLine = true,
                placeholder = { Text("Alarm Label", color = TextTertiary) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = GlassBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = GlassSurface,
                    unfocusedContainerColor = GlassSurface
                ),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("alarm_label_input")
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Repeat Selector
            Text("Repeat", color = TextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(8.dp))

            val repeatOptions = listOf("Once", "Every day", "Monday-Friday", "Saturday-Sunday", "Custom")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeatOptions.forEach { opt ->
                    val isSelected = repeatType == opt
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (isSelected) Color.White.copy(alpha = 0.25f) else GlassSurface)
                            .border(1.dp, if (isSelected) Color.White.copy(alpha = 0.5f) else GlassBorder, CircleShape)
                            .clickable {
                                haptic.buttonClick()
                                repeatType = opt
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = opt,
                            color = if (isSelected) Color.White else TextSecondary,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }

            // Custom Days Selector if "Custom" chosen
            if (repeatType == "Custom") {
                Spacer(modifier = Modifier.height(10.dp))
                val dayNames = listOf("M", "T", "W", "T", "F", "S", "S")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    (1..7).forEach { dayNum ->
                        val isDaySelected = customDays.contains(dayNum)
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (isDaySelected) accentColor.copy(alpha = 0.35f) else GlassSurface)
                                .border(1.dp, if (isDaySelected) accentColor else GlassBorder, CircleShape)
                                .clickable {
                                    haptic.buttonClick()
                                    customDays = if (isDaySelected) customDays - dayNum else customDays + dayNum
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = dayNames[dayNum - 1],
                                color = if (isDaySelected) Color.White else TextSecondary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Sound Selector & Preview & Custom Device Audio Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Sound", color = TextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    if (SoundPlayer.isCustomSound(context, selectedSound)) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(accentColor.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "CUSTOM",
                                color = accentColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Preview Button
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(GlassSurface)
                            .border(1.dp, GlassBorder, CircleShape)
                            .clickable {
                                haptic.buttonClick()
                                if (isPreviewPlaying) {
                                    SoundPlayer.stopPreview()
                                    isPreviewPlaying = false
                                } else {
                                    isPreviewPlaying = true
                                    SoundPlayer.previewSound(context, selectedSound)
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isPreviewPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = "Preview",
                                tint = accentColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isPreviewPlaying) "Stop" else "Preview",
                                color = accentColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Add Custom Ringtone Button (+)
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.2f))
                            .border(1.dp, accentColor.copy(alpha = 0.5f), CircleShape)
                            .clickable {
                                haptic.buttonClick()
                                audioPickerLauncher.launch("audio/*")
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("add_custom_ringtone_button")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add custom sound",
                                tint = accentColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Add Ringtone",
                                color = accentColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Sound pills row: Custom imported audio first, then presets
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Custom ringtones list
                customRingtones.forEach { customTone ->
                    val isSoundSelected = selectedSound == customTone.name || selectedSound == customTone.filePath
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (isSoundSelected) accentColor.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.08f))
                            .border(1.dp, if (isSoundSelected) accentColor else GlassBorder, CircleShape)
                            .clickable {
                                haptic.buttonClick()
                                selectedSound = customTone.name
                                SoundPlayer.previewSound(context, customTone.name)
                                isPreviewPlaying = true
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = if (isSoundSelected) Color.White else accentColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = customTone.name,
                                color = if (isSoundSelected) Color.White else TextSecondary,
                                fontSize = 13.sp,
                                fontWeight = if (isSoundSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                // Default built-in presets
                SoundPlayer.AVAILABLE_SOUNDS.forEach { sound ->
                    val isSoundSelected = selectedSound == sound
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (isSoundSelected) Color.White.copy(alpha = 0.25f) else GlassSurface)
                            .border(1.dp, if (isSoundSelected) Color.White.copy(alpha = 0.5f) else GlassBorder, CircleShape)
                            .clickable {
                                haptic.buttonClick()
                                selectedSound = sound
                                SoundPlayer.previewSound(context, sound)
                                isPreviewPlaying = true
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = sound,
                            color = if (isSoundSelected) Color.White else TextSecondary,
                            fontSize = 13.sp,
                            fontWeight = if (isSoundSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Vibration Switch
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(GlassSurface)
                    .border(1.dp, GlassBorder, RoundedCornerShape(18.dp))
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Vibration", color = TextPrimary, fontSize = 16.sp)
                Switch(
                    checked = vibrate,
                    onCheckedChange = {
                        haptic.toggle(it)
                        vibrate = it
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = accentColor.copy(alpha = 0.7f),
                        uncheckedThumbColor = TextTertiary,
                        uncheckedTrackColor = GlassSurface
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Snooze Switch
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(GlassSurface)
                    .border(1.dp, GlassBorder, RoundedCornerShape(18.dp))
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Snooze", color = TextPrimary, fontSize = 16.sp)
                Switch(
                    checked = snoozeEnabled,
                    onCheckedChange = {
                        haptic.toggle(it)
                        snoozeEnabled = it
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = accentColor.copy(alpha = 0.7f),
                        uncheckedThumbColor = TextTertiary,
                        uncheckedTrackColor = GlassSurface
                    )
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Action Buttons: Save & Optional Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isEditing && onDelete != null) {
                    Box(
                        modifier = Modifier
                            .weight(0.35f)
                            .height(54.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(AccentRed.copy(alpha = 0.2f))
                            .border(1.dp, AccentRed.copy(alpha = 0.4f), RoundedCornerShape(22.dp))
                            .clickable {
                                haptic.reject()
                                SoundPlayer.stopPreview()
                                onDelete(initialAlarm.id)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = AccentRed,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                LiquidGlassButton(
                    modifier = Modifier.weight(1f).height(54.dp),
                    shape = RoundedCornerShape(22.dp),
                    backgroundColor = accentColor.copy(alpha = 0.35f),
                    borderColor = accentColor,
                    onClick = {
                        SoundPlayer.stopPreview()
                        // Convert 12h display back to 24h
                        val actualHour = when {
                            !isPm && displayHour == 12 -> 0
                            !isPm -> displayHour
                            isPm && displayHour == 12 -> 12
                            else -> displayHour + 12
                        }

                        val finalAlarm = initialAlarm?.copy(
                            hour = actualHour,
                            minute = selectedMinute,
                            label = label.trim().ifEmpty { "Alarm" },
                            repeatType = repeatType,
                            customDays = customDays,
                            soundName = selectedSound,
                            vibrate = vibrate,
                            snoozeEnabled = snoozeEnabled
                        ) ?: AlarmModel(
                            hour = actualHour,
                            minute = selectedMinute,
                            label = label.trim().ifEmpty { "Alarm" },
                            isEnabled = true,
                            repeatType = repeatType,
                            customDays = customDays,
                            soundName = selectedSound,
                            vibrate = vibrate,
                            snoozeEnabled = snoozeEnabled
                        )

                        onSave(finalAlarm)
                    }
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isEditing) "Save Changes" else "Create Alarm",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
