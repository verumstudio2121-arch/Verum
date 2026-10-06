package com.example.timer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentOrange
import com.example.ui.theme.DeepBlack
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassBorderBright
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.GlassSurfaceElevated
import com.example.ui.theme.LocalAccentColor
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.util.LocalHapticManager

@Composable
fun TimerScreen(
    viewModel: TimerViewModel,
    onOpenSettings: () -> Unit = {}
) {
    val currentAccent = LocalAccentColor.current
    val accentColor = currentAccent.primary
    val state by viewModel.state.collectAsState()
    val remainingMillis by viewModel.remainingMillis.collectAsState()
    val totalMillis by viewModel.totalMillis.collectAsState()

    val pickedHours by viewModel.pickedHours.collectAsState()
    val pickedMinutes by viewModel.pickedMinutes.collectAsState()
    val pickedSeconds by viewModel.pickedSeconds.collectAsState()

    val haptic = LocalHapticManager.current
    val isIdle = state == TimerState.IDLE

    // State for Wheel vs Keypad Mode (as seen in video)
    var isKeypadMode by remember { mutableStateOf(false) }
    var focusedField by remember { mutableStateOf(TimerUnitField.MINUTES) }

    // Top action bar state
    var showMenu by remember { mutableStateOf(false) }
    var showAddPresetDialog by remember { mutableStateOf(false) }
    val presetList = remember { mutableStateListOf(1, 5, 10, 15, 30, 45, 60) }

    // Calculate progress ratio (1.0 down to 0.0)
    val targetProgress = if (totalMillis > 0) {
        (remainingMillis.toFloat() / totalMillis.toFloat()).coerceIn(0f, 1f)
    } else 1f

    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        label = "timer_progress"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlack)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar with Title, "+" and "⋮" (Matching video at 00:00)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Timer",
                    color = TextPrimary,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // "+" Add Preset button
                    IconButton(
                        onClick = {
                            haptic.buttonClick()
                            showAddPresetDialog = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Preset",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // "⋮" More options button
                    Box {
                        IconButton(
                            onClick = {
                                haptic.buttonClick()
                                showMenu = true
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More Options",
                                tint = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(GlassSurfaceElevated)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Settings", color = TextPrimary) },
                                onClick = {
                                    showMenu = false
                                    onOpenSettings()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Reset to 5 min", color = TextSecondary) },
                                onClick = {
                                    showMenu = false
                                    haptic.buttonClick()
                                    viewModel.setDuration(0, 5, 0)
                                }
                            )
                        }
                    }
                }
            }

            // Preset Timers Chips (visible in Idle state when not typing in keypad)
            AnimatedVisibility(
                visible = isIdle && !isKeypadMode,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        presetList.forEach { mins ->
                            val isSelected = pickedMinutes == mins && pickedHours == 0 && pickedSeconds == 0
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (isSelected) accentColor.copy(alpha = 0.22f) else GlassSurface)
                                    .border(
                                        1.dp,
                                        if (isSelected) accentColor.copy(alpha = 0.55f) else GlassBorder,
                                        CircleShape
                                    )
                                    .clickable {
                                        haptic.buttonClick()
                                        viewModel.selectPreset(mins)
                                    }
                                    .padding(horizontal = 16.dp, vertical = 7.dp)
                            ) {
                                Text(
                                    text = if (mins >= 60) "${mins / 60} hr" else "$mins min",
                                    color = if (isSelected) accentColor else TextSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }
            }

            // Center Content: Dual-Mode Picker (Idle) vs Countdown Ring (Running)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                if (isIdle) {
                    // High-tech Dual-Mode Samsung Clock style Timer Picker
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        SamsungStyleTimerPicker(
                            hours = pickedHours,
                            minutes = pickedMinutes,
                            seconds = pickedSeconds,
                            isKeypadMode = isKeypadMode,
                            focusedField = focusedField,
                            onKeypadModeChange = { isKeypadMode = it },
                            onFocusedFieldChange = { focusedField = it },
                            onDurationChange = { h, m, s ->
                                viewModel.setDuration(h, m, s)
                            }
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Pill "Start" button (Matching video frames 00:00 - 00:04, 00:11)
                        Box(
                            modifier = Modifier
                                .width(140.dp)
                                .height(48.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            Color(0xFF0284C7),
                                            Color(0xFF38BDF8)
                                        )
                                    )
                                )
                                .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.6f), CircleShape)
                                .clickable {
                                    haptic.confirm()
                                    isKeypadMode = false
                                    viewModel.start()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Start",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.3.sp
                            )
                        }
                    }
                } else {
                    // Countdown Ring Animation (when Running / Paused / Finished)
                    Box(
                        modifier = Modifier.size(280.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val strokeWidth = 14.dp.toPx()
                            val sweepAngle = 360f * animatedProgress

                            // Background Track
                            drawCircle(
                                color = Color.White.copy(alpha = 0.08f),
                                style = Stroke(width = strokeWidth)
                            )

                            // Foreground Progress Arc
                            drawArc(
                                brush = Brush.sweepGradient(
                                    listOf(
                                        accentColor,
                                        currentAccent.light,
                                        accentColor
                                    )
                                ),
                                startAngle = -90f,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                        }

                        // Center Time Text
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (state == TimerState.FINISHED) "00:00" else TimerViewModel.formatTime(remainingMillis),
                                color = if (state == TimerState.FINISHED) AccentOrange else TextPrimary,
                                fontSize = 52.sp,
                                fontWeight = FontWeight.Light,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = (-1).sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = when (state) {
                                    TimerState.RUNNING -> "Counting down"
                                    TimerState.PAUSED -> "Paused"
                                    TimerState.FINISHED -> "Finished"
                                    else -> ""
                                },
                                color = TextSecondary,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            // Running / Paused Action Controls (Cancel & Pause/Resume)
            if (!isIdle) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 90.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Cancel Button
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.12f))
                            .border(1.dp, GlassBorderBright, CircleShape)
                            .clickable {
                                haptic.buttonClick()
                                viewModel.cancel()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Cancel",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Action Button: PAUSE / RESUME / RESET
                    val (btnText, btnColor) = when (state) {
                        TimerState.RUNNING -> "Pause" to AccentOrange
                        TimerState.PAUSED -> "Resume" to AccentGreen
                        TimerState.FINISHED -> "Reset" to AccentGreen
                        else -> "Start" to accentColor
                    }

                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(btnColor.copy(alpha = 0.22f))
                            .border(1.dp, btnColor.copy(alpha = 0.55f), CircleShape)
                            .clickable {
                                when (state) {
                                    TimerState.RUNNING -> {
                                        haptic.buttonClick()
                                        viewModel.pause()
                                    }
                                    TimerState.PAUSED -> {
                                        haptic.confirm()
                                        viewModel.resume()
                                    }
                                    TimerState.FINISHED -> {
                                        haptic.buttonClick()
                                        viewModel.cancel()
                                    }
                                    else -> {}
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = btnText,
                            color = btnColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(76.dp))
            }
        }
    }

    // Add Preset Dialog
    if (showAddPresetDialog) {
        var newPresetMins by remember { mutableStateOf("20") }
        AlertDialog(
            onDismissRequest = { showAddPresetDialog = false },
            containerColor = GlassSurfaceElevated,
            title = { Text("Add Quick Timer Preset", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Select or enter duration in minutes:", color = TextSecondary, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        listOf(2, 3, 20, 25, 50, 90).forEach { m ->
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (newPresetMins == m.toString()) accentColor.copy(alpha = 0.22f) else GlassSurface)
                                    .border(1.dp, if (newPresetMins == m.toString()) accentColor else GlassBorder, CircleShape)
                                    .clickable { newPresetMins = m.toString() }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("$m min", color = TextPrimary, fontSize = 12.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val mins = newPresetMins.toIntOrNull() ?: 20
                        if (!presetList.contains(mins)) {
                            presetList.add(mins)
                            presetList.sort()
                        }
                        viewModel.selectPreset(mins)
                        showAddPresetDialog = false
                    }
                ) {
                    Text("Add & Select", color = accentColor, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddPresetDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}
