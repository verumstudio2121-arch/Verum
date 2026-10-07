package com.example.alarm.ui

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.alarm.AlarmReceiver
import com.example.alarm.AlarmRingingActivity
import com.example.data.AlarmModel
import com.example.data.AlarmRepository
import com.example.data.AppSettings
import com.example.ui.theme.AccentOrange
import com.example.ui.theme.AccentRed
import com.example.ui.theme.DeepBlack
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassBorderBright
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.GlassSurfaceElevated
import com.example.ui.theme.LiquidGlassCard
import com.example.ui.theme.LocalAccentColor
import com.example.ui.theme.TextDisabled
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.util.LocalHapticManager

@Composable
fun AlarmScreen(
    repository: AlarmRepository,
    settings: AppSettings,
    onOpenSettings: () -> Unit
) {
    val alarms by repository.alarms.collectAsState()
    var isEditMode by remember { mutableStateOf(false) }
    var editingAlarm by remember { mutableStateOf<AlarmModel?>(null) }
    var isCreatingAlarm by remember { mutableStateOf(false) }
    val haptic = LocalHapticManager.current
    val context = LocalContext.current
    val accentColor = LocalAccentColor.current.primary

    // Request POST_NOTIFICATIONS permission on Android 13+
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlack)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp)
        ) {
            // Top Navigation Bar: [ Edit ]  [ + ]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(GlassSurface)
                        .border(1.dp, GlassBorder, CircleShape)
                        .clickable {
                            haptic.buttonClick()
                            isEditMode = !isEditMode
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = if (isEditMode) "Done" else "Edit",
                        color = if (isEditMode) Color.White else TextSecondary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Quick Test Alarm button (launches full-screen alarm ringing experience immediately)
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(GlassSurface)
                            .border(1.dp, GlassBorder, CircleShape)
                            .clickable {
                                haptic.confirm()
                                val testIntent = Intent(context, AlarmRingingActivity::class.java).apply {
                                    putExtra(AlarmRingingActivity.EXTRA_ALARM_ID, "test_preview")
                                    putExtra(AlarmRingingActivity.EXTRA_ALARM_LABEL, "Wake Up")
                                    putExtra(AlarmRingingActivity.EXTRA_ALARM_SOUND, settings.defaultAlarmSound)
                                    putExtra(AlarmRingingActivity.EXTRA_ALARM_VIBRATE, settings.vibrationEnabled)
                                    putExtra(AlarmRingingActivity.EXTRA_ALARM_SNOOZE_ENABLED, true)
                                }
                                context.startActivity(testIntent)
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = "Test Alarm",
                                tint = accentColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Test Ring",
                                color = accentColor,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Settings Button
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(GlassSurface)
                            .border(1.dp, GlassBorder, CircleShape)
                            .clickable {
                                haptic.buttonClick()
                                onOpenSettings()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Add Alarm [ + ]
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(GlassSurfaceElevated)
                            .border(1.dp, GlassBorderBright, CircleShape)
                            .clickable {
                                haptic.buttonClick()
                                isCreatingAlarm = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Alarm",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // Screen Title
            Text(
                text = "Alarms",
                color = TextPrimary,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            // Alarms List
            if (alarms.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Alarm,
                            contentDescription = null,
                            tint = TextTertiary,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No Alarms", color = TextSecondary, fontSize = 20.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Tap + above to create an alarm", color = TextTertiary, fontSize = 14.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(alarms, key = { it.id }) { alarm ->
                        val is24H = settings.clockFormat == "24 hour"
                        val textColor = if (alarm.isEnabled) TextPrimary else TextDisabled
                        val secondaryTextColor = if (alarm.isEnabled) TextSecondary else TextDisabled

                        LiquidGlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    haptic.buttonClick()
                                    editingAlarm = alarm
                                },
                            cornerRadius = 28.dp,
                            backgroundColor = GlassSurface
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Delete button in Edit mode
                                if (isEditMode) {
                                    IconButton(
                                        onClick = {
                                            haptic.reject()
                                            repository.deleteAlarm(alarm.id)
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = AccentRed
                                        )
                                    }
                                }

                                // Alarm time & details
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.Bottom) {
                                        Text(
                                            text = alarm.getFormattedTime(is24H),
                                            color = textColor,
                                            fontSize = 48.sp,
                                            fontWeight = FontWeight.Light,
                                            letterSpacing = (-1).sp
                                        )
                                        if (!is24H) {
                                             Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = alarm.getAmPm(),
                                                color = secondaryTextColor,
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Normal,
                                                modifier = Modifier.padding(bottom = 8.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // Label, Repeat badge, and Sound
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = alarm.label,
                                            color = textColor,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Medium
                                        )

                                        Text("•", color = TextTertiary, fontSize = 12.sp)

                                        Text(
                                            text = alarm.getRepeatSummary(),
                                            color = secondaryTextColor,
                                            fontSize = 13.sp
                                        )

                                        Text("•", color = TextTertiary, fontSize = 12.sp)

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.MusicNote,
                                                contentDescription = null,
                                                tint = secondaryTextColor,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text(
                                                text = alarm.soundName,
                                                color = secondaryTextColor,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }

                                // Enable / Disable switch (when not in edit mode)
                                if (!isEditMode) {
                                    Switch(
                                        checked = alarm.isEnabled,
                                        onCheckedChange = { checked ->
                                            haptic.toggle(checked)
                                            repository.toggleAlarm(alarm.id, checked)
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = accentColor.copy(alpha = 0.85f),
                                            uncheckedThumbColor = TextTertiary,
                                            uncheckedTrackColor = GlassSurface
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Add Alarm Bottom Sheet
        if (isCreatingAlarm) {
            CreateEditAlarmBottomSheet(
                initialAlarm = null,
                is24Hour = settings.clockFormat == "24 hour",
                onSave = { newAlarm ->
                    repository.addAlarm(newAlarm)
                    isCreatingAlarm = false
                },
                onDismiss = { isCreatingAlarm = false }
            )
        }

        // Edit Alarm Bottom Sheet
        editingAlarm?.let { alarm ->
            CreateEditAlarmBottomSheet(
                initialAlarm = alarm,
                is24Hour = settings.clockFormat == "24 hour",
                onSave = { updated ->
                    repository.updateAlarm(updated)
                    editingAlarm = null
                },
                onDelete = { id ->
                    repository.deleteAlarm(id)
                    editingAlarm = null
                },
                onDismiss = { editingAlarm = null }
            )
        }
    }
}
