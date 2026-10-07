package com.example.clock

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
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
import com.example.ui.theme.GlassSurfaceElevated
import com.example.ui.theme.LocalAccentColor
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.util.LocalHapticManager
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.TimeZone
import kotlin.math.cos
import kotlin.math.sin

enum class ClockFaceType {
    ANALOG,
    DIGITAL
}

/**
 * Premium Minimal Liquid Glass Hero Clock:
 * Displays an elegant Analog Clock that smoothly morphs into a Large Digital Clock
 * when tapped (as demonstrated in user video 00:38 - 00:47).
 */
@Composable
fun InteractiveHeroClock(
    modifier: Modifier = Modifier,
    initialMode: ClockFaceType = ClockFaceType.ANALOG
) {
    val haptic = LocalHapticManager.current
    val accentColor = LocalAccentColor.current.primary
    var clockMode by remember { mutableStateOf(initialMode) }

    // Live continuous clock tick state
    var currentMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            currentMillis = System.currentTimeMillis()
            delay(50) // 20fps for smooth sweeping second hand
        }
    }

    val now = remember(currentMillis) { ZonedDateTime.now() }
    val timeZone = remember { TimeZone.getDefault() }
    val tzName = remember {
        val longName = timeZone.getDisplayName(false, TimeZone.LONG, Locale.getDefault())
        if (longName.length > 24) timeZone.getDisplayName(false, TimeZone.SHORT, Locale.getDefault()) else longName
    }
    val dateText = remember(now) {
        now.format(DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault()))
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                haptic.buttonClick()
                clockMode = if (clockMode == ClockFaceType.ANALOG) ClockFaceType.DIGITAL else ClockFaceType.ANALOG
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Animated transition between Analog and Digital faces
        AnimatedContent(
            targetState = clockMode,
            transitionSpec = {
                (fadeIn(animationSpec = tween(280, easing = FastOutSlowInEasing)) +
                        scaleIn(initialScale = 0.90f, animationSpec = tween(280)))
                    .togetherWith(
                        fadeOut(animationSpec = tween(200)) +
                                scaleOut(targetScale = 0.92f, animationSpec = tween(200))
                    )
            },
            label = "clockFaceTransition"
        ) { mode ->
            when (mode) {
                ClockFaceType.ANALOG -> {
                    AnalogClockDial(
                        now = now,
                        accentColor = accentColor,
                        size = 230.dp
                    )
                }

                ClockFaceType.DIGITAL -> {
                    DigitalClockHero(
                        now = now,
                        accentColor = accentColor
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Subtitle: Time Zone | Date (e.g. "India Standard Time | Wed 7 Oct")
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "$tzName | $dateText",
                color = TextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.2.sp
            )
        }
    }
}

/**
 * Minimalist Liquid Glass Analog Clock Dial (As seen in video 00:38 - 00:39 & 00:42)
 */
@Composable
private fun AnalogClockDial(
    now: ZonedDateTime,
    accentColor: Color,
    size: Dp = 230.dp
) {
    val density = LocalDensity.current
    val hour = now.hour % 12
    val minute = now.minute
    val second = now.second
    val nano = now.nano

    // Continuous smooth fractional angles
    val secondFraction = second + (nano / 1_000_000_000f)
    val secondAngle = (secondFraction / 60f) * 360f

    val minuteFraction = minute + (secondFraction / 60f)
    val minuteAngle = (minuteFraction / 60f) * 360f

    val hourFraction = hour + (minuteFraction / 60f)
    val hourAngle = (hourFraction / 12f) * 360f

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF1E212B).copy(alpha = 0.85f),
                        Color(0xFF101217).copy(alpha = 0.95f),
                        Color(0xFF0A0C10)
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

            // Hour numbers 1 to 12
            val paint = android.graphics.Paint().apply {
                isAntiAlias = true
                textSize = density.run { 13.sp.toPx() }
                color = android.graphics.Color.argb(200, 200, 205, 215)
                textAlign = android.graphics.Paint.Align.CENTER
                typeface = android.graphics.Typeface.create(
                    android.graphics.Typeface.DEFAULT,
                    android.graphics.Typeface.NORMAL
                )
            }

            val numRadius = radius * 0.78f
            for (i in 1..12) {
                val angleDeg = (i * 30.0) - 90.0
                val angleRad = Math.toRadians(angleDeg)
                val x = center.x + (numRadius * cos(angleRad)).toFloat()
                val y = center.y + (numRadius * sin(angleRad)).toFloat() - (paint.descent() + paint.ascent()) / 2f
                drawContext.canvas.nativeCanvas.drawText(i.toString(), x, y, paint)
            }

            // 60 Subtle Tick Marks
            for (i in 0 until 60) {
                val isHourTick = i % 5 == 0
                val tickLength = if (isHourTick) radius * 0.09f else radius * 0.045f
                val tickWidth = if (isHourTick) 2.dp.toPx() else 1.dp.toPx()
                val tickColor = if (isHourTick) Color.White.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.18f)

                val angleDeg = (i * 6.0) - 90.0
                val angleRad = Math.toRadians(angleDeg)

                val startRadius = radius * 0.92f - tickLength
                val endRadius = radius * 0.92f

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

            // Hour Hand (White pill with depth)
            rotate(degrees = hourAngle, pivot = center) {
                val hourLength = radius * 0.52f
                drawLine(
                    color = Color.White,
                    start = Offset(center.x, center.y + (radius * 0.06f)),
                    end = Offset(center.x, center.y - hourLength),
                    strokeWidth = 5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // Minute Hand (Sleek white pill)
            rotate(degrees = minuteAngle, pivot = center) {
                val minuteLength = radius * 0.74f
                drawLine(
                    color = Color.White.copy(alpha = 0.95f),
                    start = Offset(center.x, center.y + (radius * 0.08f)),
                    end = Offset(center.x, center.y - minuteLength),
                    strokeWidth = 3.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // Second Hand (Vibrant Electric Blue needle with tail & counterweight)
            rotate(degrees = secondAngle, pivot = center) {
                val secondLength = radius * 0.88f
                val tailLength = radius * 0.22f

                // Counterweight tail
                drawLine(
                    color = accentColor,
                    start = Offset(center.x, center.y + tailLength),
                    end = Offset(center.x, center.y - secondLength),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // Blue pivot circle
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
 * Large Liquid Glass Digital Clock Hero (As seen in video 00:40 & 00:44)
 * Displays big crisp time digits with glowing blue seconds.
 */
@Composable
private fun DigitalClockHero(
    now: ZonedDateTime,
    accentColor: Color
) {
    val hour12 = when (val h = now.hour % 12) {
        0 -> 12
        else -> h
    }
    val minuteStr = String.format("%02d", now.minute)
    val secondStr = String.format("%02d", now.second)
    val amPm = if (now.hour < 12) "AM" else "PM"

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
                        Color(0xFF0A0C10)
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
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center
            ) {
                // Hours & Minutes
                Text(
                    text = "$hour12:$minuteStr:",
                    color = TextPrimary,
                    fontSize = 58.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-1.5).sp
                )

                // Seconds in vibrant accent electric blue
                Text(
                    text = secondStr,
                    color = accentColor,
                    fontSize = 58.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-1.5).sp
                )

                Spacer(modifier = Modifier.size(6.dp))

                // AM / PM
                Text(
                    text = amPm,
                    color = accentColor.copy(alpha = 0.85f),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Subtle "Tap to switch to analog" pill
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(GlassSurface)
                    .border(1.dp, GlassBorder, CircleShape)
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Tap to switch clock style",
                    color = TextTertiary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }
    }
}
