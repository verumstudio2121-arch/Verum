package com.example.stopwatch

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentRed
import com.example.ui.theme.DeepBlack
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassBorderBright
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.GlassSurfaceElevated
import com.example.ui.theme.LiquidGlassCard
import com.example.ui.theme.LocalAccentColor
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.util.LocalHapticManager

@Composable
fun StopwatchScreen(
    viewModel: StopwatchViewModel
) {
    val isRunning by viewModel.isRunning.collectAsState()
    val elapsedMillis by viewModel.elapsedMillis.collectAsState()
    val laps by viewModel.laps.collectAsState()
    val haptic = LocalHapticManager.current

    val formattedDisplay = StopwatchViewModel.formatTime(elapsedMillis)

    // Calculate fastest and slowest lap for highlighting
    val minLapTime = if (laps.size >= 2) laps.minOfOrNull { it.lapTimeMillis } else null
    val maxLapTime = if (laps.size >= 2) laps.maxOfOrNull { it.lapTimeMillis } else null

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlack)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Title
            Text(
                text = "Stopwatch",
                color = TextPrimary,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(top = 16.dp, bottom = 24.dp)
            )

            // Interactive Hero Stopwatch Face (Toggles between Chronograph Dial and Digital Display on tap)
            InteractiveStopwatchFace(
                elapsedMillis = elapsedMillis,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            )

            // Dual Tactile Glass Action Buttons (Matching reference video)
            val accentColor = LocalAccentColor.current.primary
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp, horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Button: LAP (Flag) / RESET (Refresh)
                val canLapOrReset = elapsedMillis > 0

                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(
                            if (canLapOrReset) GlassSurfaceElevated else GlassSurface
                        )
                        .border(
                            1.dp,
                            if (canLapOrReset) GlassBorderBright else GlassBorder,
                            CircleShape
                        )
                        .clickable(enabled = canLapOrReset) {
                            if (isRunning) {
                                haptic.sliderTick()
                                viewModel.lap()
                            } else {
                                haptic.buttonClick()
                                viewModel.reset()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (isRunning) Icons.Default.Flag else Icons.Default.Refresh,
                            contentDescription = if (isRunning) "Lap" else "Reset",
                            tint = if (canLapOrReset) Color.White else TextTertiary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isRunning) "Lap" else "Reset",
                            color = if (canLapOrReset) Color.White else TextTertiary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Right Button: START (Play) / PAUSE (Pause)
                val rightButtonBg = if (isRunning) accentColor.copy(alpha = 0.28f) else accentColor
                val rightButtonBorder = if (isRunning) accentColor else accentColor.copy(alpha = 0.8f)
                val rightIconTint = if (isRunning) accentColor else Color.White

                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(rightButtonBg)
                        .border(1.5.dp, rightButtonBorder, CircleShape)
                        .clickable {
                            if (isRunning) {
                                haptic.buttonClick()
                                viewModel.pause()
                            } else {
                                haptic.confirm()
                                viewModel.start()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isRunning) "Pause" else "Start",
                        tint = rightIconTint,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Lap History Table
            if (laps.isNotEmpty()) {
                LiquidGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 24.dp,
                    backgroundColor = GlassSurface
                ) {
                    // Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Lap", color = TextTertiary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Text("Lap Time", color = TextTertiary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Text("Overall Split", color = TextTertiary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp),
                        contentPadding = PaddingValues(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(laps, key = { it.lapIndex }) { lap ->
                            val isFastest = minLapTime != null && lap.lapTimeMillis == minLapTime
                            val isSlowest = maxLapTime != null && lap.lapTimeMillis == maxLapTime

                            val lapColor = when {
                                isFastest -> AccentGreen
                                isSlowest -> AccentRed
                                else -> TextPrimary
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = String.format("Lap %02d", lap.lapIndex),
                                    color = lapColor,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = StopwatchViewModel.formatTime(lap.lapTimeMillis),
                                    color = lapColor,
                                    fontSize = 15.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = StopwatchViewModel.formatTime(lap.splitTimeMillis),
                                    color = TextSecondary,
                                    fontSize = 15.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
