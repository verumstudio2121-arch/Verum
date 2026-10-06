package com.example.alarm.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    onSave: (AlarmModel) -> Unit,
    onDelete: ((String) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val haptic = LocalHapticManager.current
    val accentColor = LocalAccentColor.current.primary

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
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header: Cancel / Title / Save
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Cancel",
                    color = TextSecondary,
                    fontSize = 16.sp,
                    modifier = Modifier.clickable {
                        SoundPlayer.stopPreview()
                        haptic.buttonClick()
                        onDismiss()
                    }
                )

                Text(
                    text = if (isEditing) "Edit Alarm" else "Add Alarm",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Save",
                    color = accentColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable {
                        SoundPlayer.stopPreview()
                        haptic.confirm()

                        val final24Hour = when {
                            !isPm && displayHour == 12 -> 0
                            isPm && displayHour < 12 -> displayHour + 12
                            else -> displayHour
                        }

                        val savedAlarm = AlarmModel(
                            id = initialAlarm?.id ?: java.util.UUID.randomUUID().toString(),
                            hour = final24Hour,
                            minute = selectedMinute,
                            label = label.ifBlank { "Alarm" },
                            isEnabled = true,
                            repeatType = repeatType,
                            customDays = customDays,
                            soundName = selectedSound,
                            vibrate = vibrate,
                            snoozeEnabled = snoozeEnabled
                        )
                        onSave(savedAlarm)
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Liquid Glass Time Selector
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 28.dp,
                backgroundColor = GlassSurfaceElevated
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Hour selector with + / -
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(GlassSurface)
                                .clickable {
                                    haptic.sliderTick()
                                    displayHour = if (displayHour == 12) 1 else displayHour + 1
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("▲", color = TextSecondary, fontSize = 12.sp)
                        }

                        Text(
                            text = String.format("%02d", displayHour),
                            color = TextPrimary,
                            fontSize = 54.sp,
                            fontWeight = FontWeight.Light,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(GlassSurface)
                                .clickable {
                                    haptic.sliderTick()
                                    displayHour = if (displayHour == 1) 12 else displayHour - 1
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("▼", color = TextSecondary, fontSize = 12.sp)
                        }
                    }

                    Text(
                        text = ":",
                        color = TextSecondary,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Light,
                        modifier = Modifier.padding(horizontal = 14.dp)
                    )

                    // Minute selector with + / -
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(GlassSurface)
                                .clickable {
                                    haptic.sliderTick()
                                    selectedMinute = (selectedMinute + 1) % 60
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("▲", color = TextSecondary, fontSize = 12.sp)
                        }

                        Text(
                            text = String.format("%02d", selectedMinute),
                            color = TextPrimary,
                            fontSize = 54.sp,
                            fontWeight = FontWeight.Light,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(GlassSurface)
                                .clickable {
                                    haptic.sliderTick()
                                    selectedMinute = if (selectedMinute == 0) 59 else selectedMinute - 1
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("▼", color = TextSecondary, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    // AM / PM Toggle Capsule
                    Column(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(GlassSurface)
                            .border(1.dp, GlassBorder, CircleShape)
                            .padding(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (!isPm) Color.White.copy(alpha = 0.28f) else Color.Transparent)
                                .clickable {
                                    haptic.toggle(false)
                                    isPm = false
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "AM",
                                color = if (!isPm) Color.White else TextTertiary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (isPm) Color.White.copy(alpha = 0.28f) else Color.Transparent)
                                .clickable {
                                    haptic.toggle(true)
                                    isPm = true
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "PM",
                                color = if (isPm) Color.White else TextTertiary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Label Field
            Text("Label", color = TextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(GlassSurface),
                placeholder = { Text("Alarm label (e.g. Wake Up, School, Workout)", color = TextTertiary) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GlassBorderBright,
                    unfocusedBorderColor = GlassBorder,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color.White
                ),
                shape = RoundedCornerShape(18.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Repeat Options
            Text("Repeat", color = TextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(8.dp))
            val repeatOptions = listOf("Once", "Every day", "Monday-Friday", "Saturday-Sunday", "Custom")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
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

            // Sound Selector & Preview
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Sound", color = TextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
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
                                SoundPlayer.previewSound(selectedSound)
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
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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
                                SoundPlayer.previewSound(sound)
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

            // Delete button when editing
            if (isEditing && onDelete != null && initialAlarm != null) {
                Spacer(modifier = Modifier.height(24.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(CircleShape)
                        .background(AccentRed.copy(alpha = 0.15f))
                        .border(1.dp, AccentRed.copy(alpha = 0.35f), CircleShape)
                        .clickable {
                            SoundPlayer.stopPreview()
                            haptic.reject()
                            onDelete(initialAlarm.id)
                        }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AccentRed, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Delete Alarm", color = AccentRed, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
