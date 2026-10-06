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
import com.example.ui.theme.LiquidGlassCard
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

            // Large Precision Monospace Time Display
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 28.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = formattedDisplay,
                    color = TextPrimary,
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Light,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = (-1).sp
                )
            }

            // Dual Tactile Glass Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Button: LAP / RESET
                val canLapOrReset = elapsedMillis > 0
                val leftButtonText = if (isRunning) "LAP" else "RESET"

                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .clip(CircleShape)
                        .background(
                            if (canLapOrReset) Color.White.copy(alpha = 0.12f) else GlassSurface
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
                    Text(
                        text = leftButtonText,
                        color = if (canLapOrReset) Color.White else TextTertiary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp
                    )
                }

                // Right Button: START / STOP
                val isStop = isRunning
                val rightButtonBg = if (isStop) AccentRed.copy(alpha = 0.22f) else AccentGreen.copy(alpha = 0.22f)
                val rightButtonBorder = if (isStop) AccentRed.copy(alpha = 0.55f) else AccentGreen.copy(alpha = 0.55f)
                val rightButtonText = if (isStop) "STOP" else "START"
                val rightTextColor = if (isStop) AccentRed else AccentGreen

                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .clip(CircleShape)
                        .background(rightButtonBg)
                        .border(1.dp, rightButtonBorder, CircleShape)
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
                    Text(
                        text = rightButtonText,
                        color = rightTextColor,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp
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
