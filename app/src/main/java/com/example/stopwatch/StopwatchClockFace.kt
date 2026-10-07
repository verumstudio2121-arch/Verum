package com.example.stopwatch

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassBorderBright
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.LocalAccentColor
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.util.LocalHapticManager
import kotlin.math.cos
import kotlin.math.sin

enum class StopwatchFaceMode {
    CHRONOGRAPH_ANALOG,
    DIGITAL
}

/**
 * Interactive Dual-Mode Stopwatch Face:
 * Flips between the Analog Chronograph Stopwatch Dial and the Minimal Liquid Glass Digital Stopwatch
 * when tapped (as requested: "do same in the stopwatch section").
 */
@Composable
fun InteractiveStopwatchFace(
    elapsedMillis: Long,
    modifier: Modifier = Modifier,
    initialMode: StopwatchFaceMode = StopwatchFaceMode.CHRONOGRAPH_ANALOG
) {
    val haptic = LocalHapticManager.current
    val accentColor = LocalAccentColor.current.primary
    var faceMode by remember { mutableStateOf(initialMode) }

    val formattedTime = remember(elapsedMillis) {
        StopwatchViewModel.formatTime(elapsedMillis)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                haptic.buttonClick()
                faceMode = if (faceMode == StopwatchFaceMode.CHRONOGRAPH_ANALOG) {
                    StopwatchFaceMode.DIGITAL
                } else {
                    StopwatchFaceMode.CHRONOGRAPH_ANALOG
                }
            },
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = faceMode,
            transitionSpec = {
                (fadeIn(animationSpec = tween(280, easing = FastOutSlowInEasing)) +
                        scaleIn(initialScale = 0.90f, animationSpec = tween(280)))
                    .togetherWith(
                        fadeOut(animationSpec = tween(200)) +
                                scaleOut(targetScale = 0.92f, animationSpec = tween(200))
                    )
            },
            label = "stopwatchFaceTransition"
        ) { mode ->
            when (mode) {
                StopwatchFaceMode.CHRONOGRAPH_ANALOG -> {
                    ChronographAnalogDial(
                        elapsedMillis = elapsedMillis,
                        formattedTime = formattedTime,
                        accentColor = accentColor,
                        size = 250.dp
                    )
                }

                StopwatchFaceMode.DIGITAL -> {
                    MinimalDigitalStopwatch(
                        formattedTime = formattedTime,
                        accentColor = accentColor
                    )
                }
            }
        }
    }
}

/**
 * Dual-Dial Analog Chronograph Stopwatch Dial (As seen in video 00:19 - 00:37)
 * Outer dial: 60 seconds with 5-second markers, tick marks, and sweeping blue hand.
 * Sub-dial: 30 minutes with small needle.
 * Upper digital readout: 00:00.00
 */
@Composable
private fun ChronographAnalogDial(
    elapsedMillis: Long,
    formattedTime: String,
    accentColor: Color,
    size: Dp = 250.dp
) {
    val density = LocalDensity.current

    // Angles
    val secondsTotal = elapsedMillis / 1000f
    val secondAngle = (secondsTotal % 60f) / 60f * 360f

    val minutesTotal = elapsedMillis / 60000f
    val minuteSubAngle = (minutesTotal % 30f) / 30f * 360f

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF1E212B).copy(alpha = 0.85f),
                        Color(0xFF101217).copy(alpha = 0.95f),
                        Color(0xFF090A0E)
                    )
                )
            )
            .border(1.5.dp, GlassBorderBright, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val radius = this.size.width / 2f

            // Outer dial rim specular highlight
            drawCircle(
                color = Color.White.copy(alpha = 0.05f),
                radius = radius - 2f,
                style = Stroke(width = 2f)
            )

            // Outer Dial Numbers (5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55, 60)
            val outerPaint = android.graphics.Paint().apply {
                isAntiAlias = true
                textSize = density.run { 11.5.sp.toPx() }
                color = android.graphics.Color.argb(210, 210, 215, 225)
                textAlign = android.graphics.Paint.Align.CENTER
                typeface = android.graphics.Typeface.create(
                    android.graphics.Typeface.DEFAULT,
                    android.graphics.Typeface.NORMAL
                )
            }

            val numRadius = radius * 0.82f
            val numbers = listOf(60, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55)
            for (i in 0 until 12) {
                val num = numbers[i]
                val angleDeg = (i * 30.0) - 90.0
                val angleRad = Math.toRadians(angleDeg)
                val x = center.x + (numRadius * cos(angleRad)).toFloat()
                val y = center.y + (numRadius * sin(angleRad)).toFloat() - (outerPaint.descent() + outerPaint.ascent()) / 2f
                drawContext.canvas.nativeCanvas.drawText(String.format("%02d", num), x, y, outerPaint)
            }

            // Precision Tick Marks (60 major, 240 split ticks)
            for (i in 0 until 120) {
                val isSecondTick = i % 2 == 0
                val isMajorTick = i % 10 == 0

                val tickLength = when {
                    isMajorTick -> radius * 0.08f
                    isSecondTick -> radius * 0.05f
                    else -> radius * 0.025f
                }
                val tickWidth = if (isMajorTick) 1.8.dp.toPx() else 1.dp.toPx()
                val tickColor = when {
                    isMajorTick -> Color.White.copy(alpha = 0.6f)
                    isSecondTick -> Color.White.copy(alpha = 0.3f)
                    else -> Color.White.copy(alpha = 0.12f)
                }

                val angleDeg = (i * 3.0) - 90.0
                val angleRad = Math.toRadians(angleDeg)

                val startRadius = radius * 0.94f - tickLength
                val endRadius = radius * 0.94f

                val startX = center.x + (startRadius * cos(angleRad)).toFloat()
                val startY = center.y + (startRadius * sin(angleRad)).toFloat()
                val endX = center.x + (endRadius * cos(angleRad)).toFloat()
                val endY = center.y + (endRadius * sin(angleRad)).toFloat()

                drawLine(
                    color = tickColor,
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = tickWidth,
                    cap = StrokeCap.Round
                )
            }

            // Digital Time Readout inside top half of dial (Matching reference video 00:20)
            val digitalPaint = android.graphics.Paint().apply {
                isAntiAlias = true
                textSize = density.run { 22.sp.toPx() }
                color = android.graphics.Color.WHITE
                textAlign = android.graphics.Paint.Align.CENTER
                typeface = android.graphics.Typeface.create(
                    android.graphics.Typeface.MONOSPACE,
                    android.graphics.Typeface.BOLD
                )
            }
            val digitalY = center.y - (radius * 0.42f)
            drawContext.canvas.nativeCanvas.drawText(formattedTime, center.x, digitalY, digitalPaint)

            // Inner Sub-Dial for 30 Minutes (Positioned below center)
            val subCenter = Offset(center.x, center.y + (radius * 0.38f))
            val subRadius = radius * 0.32f

            // Sub-dial background circle
            drawCircle(
                color = Color.White.copy(alpha = 0.04f),
                radius = subRadius,
                center = subCenter
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.18f),
                radius = subRadius,
                center = subCenter,
                style = Stroke(width = 1f)
            )

            // Sub-dial numerals: 5, 10, 15, 20, 25, 30
            val subPaint = android.graphics.Paint().apply {
                isAntiAlias = true
                textSize = density.run { 8.sp.toPx() }
                color = android.graphics.Color.argb(190, 190, 195, 205)
                textAlign = android.graphics.Paint.Align.CENTER
            }

            val subNums = listOf(30, 5, 10, 15, 20, 25)
            for (i in 0 until 6) {
                val num = subNums[i]
                val angleDeg = (i * 60.0) - 90.0
                val angleRad = Math.toRadians(angleDeg)
                val sx = subCenter.x + ((subRadius * 0.72f) * cos(angleRad)).toFloat()
                val sy = subCenter.y + ((subRadius * 0.72f) * sin(angleRad)).toFloat() - (subPaint.descent() + subPaint.ascent()) / 2f
                drawContext.canvas.nativeCanvas.drawText(num.toString(), sx, sy, subPaint)
            }

            // Sub-dial ticks
            for (i in 0 until 30) {
                val isFive = i % 5 == 0
                val tickLen = if (isFive) subRadius * 0.16f else subRadius * 0.09f
                val angleDeg = (i * 12.0) - 90.0
                val angleRad = Math.toRadians(angleDeg)
                val startR = subRadius * 0.94f - tickLen
                val endR = subRadius * 0.94f

                val sx = subCenter.x + (startR * cos(angleRad)).toFloat()
                val sy = subCenter.y + (startR * sin(angleRad)).toFloat()
                val ex = subCenter.x + (endR * cos(angleRad)).toFloat()
                val ey = subCenter.y + (endR * sin(angleRad)).toFloat()

                drawLine(
                    color = Color.White.copy(alpha = if (isFive) 0.5f else 0.2f),
                    start = Offset(sx, sy),
                    end = Offset(ex, ey),
                    strokeWidth = 1f
                )
            }

            // Sub-dial Minute Needle
            rotate(degrees = minuteSubAngle, pivot = subCenter) {
                drawLine(
                    color = Color.White.copy(alpha = 0.95f),
                    start = Offset(subCenter.x, subCenter.y + (subRadius * 0.15f)),
                    end = Offset(subCenter.x, subCenter.y - (subRadius * 0.65f)),
                    strokeWidth = 1.6.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawCircle(
                    color = Color.White,
                    radius = 2.dp.toPx(),
                    center = subCenter
                )
            }

            // Main Sweeping Blue Stopwatch Hand
            rotate(degrees = secondAngle, pivot = center) {
                val handLength = radius * 0.90f
                val tailLength = radius * 0.22f

                // Tail counterweight
                drawLine(
                    color = accentColor,
                    start = Offset(center.x, center.y + tailLength),
                    end = Offset(center.x, center.y - handLength),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // Accent center pivot
                drawCircle(
                    color = accentColor,
                    radius = 4.5.dp.toPx(),
                    center = center
                )
            }

            // Central White Pin Cap
            drawCircle(
                color = Color.White,
                radius = 2.dp.toPx(),
                center = center
            )
        }
    }
}

/**
 * Minimalist Liquid Glass Digital Stopwatch (Alternative mode toggled via tap)
 */
@Composable
private fun MinimalDigitalStopwatch(
    formattedTime: String,
    accentColor: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(230.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF1E212B).copy(alpha = 0.75f),
                        Color(0xFF101217).copy(alpha = 0.95f),
                        Color(0xFF090A0E)
                    )
                )
            )
            .border(1.5.dp, GlassBorderBright, RoundedCornerShape(32.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Large Glowing Monospace Digits
            Text(
                text = formattedTime,
                color = TextPrimary,
                fontSize = 58.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = (-1).sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Tap hint capsule
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(GlassSurface)
                    .border(1.dp, GlassBorder, CircleShape)
                    .padding(horizontal = 14.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "Tap to switch to chronograph dial",
                    color = TextTertiary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }
    }
}
