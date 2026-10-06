package com.example.timer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.LocalAccentColor
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.util.HapticManager
import com.example.util.LocalHapticManager
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

enum class TimerUnitField {
    HOURS,
    MINUTES,
    SECONDS
}

/**
 * High-tech Dual-Mode Samsung Clock style Timer Picker:
 * 1. Vertical scrolling wheel columns for Hours (00-99), Minutes (00-59), Seconds (00-59)
 * 2. Tap-to-type direct keypad mode with glowing focus box around the active unit.
 */
@Composable
fun SamsungStyleTimerPicker(
    hours: Int,
    minutes: Int,
    seconds: Int,
    isKeypadMode: Boolean,
    focusedField: TimerUnitField,
    onKeypadModeChange: (Boolean) -> Unit,
    onFocusedFieldChange: (TimerUnitField) -> Unit,
    onDurationChange: (hours: Int, minutes: Int, seconds: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticManager.current

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!isKeypadMode) {
            // WHEEL PICKER MODE (As seen in video 00:00 - 00:04 and 00:11)
            WheelModeDisplay(
                hours = hours,
                minutes = minutes,
                seconds = seconds,
                onHoursChange = { onDurationChange(it, minutes, seconds) },
                onMinutesChange = { onDurationChange(hours, it, seconds) },
                onSecondsChange = { onDurationChange(hours, minutes, it) },
                onTapField = { field ->
                    haptic.buttonClick()
                    onFocusedFieldChange(field)
                    onKeypadModeChange(true)
                },
                haptic = haptic
            )
        } else {
            // DIRECT KEYPAD INPUT MODE (As seen in video 00:05 - 00:10)
            KeypadModeDisplay(
                hours = hours,
                minutes = minutes,
                seconds = seconds,
                focusedField = focusedField,
                onSelectField = { field ->
                    haptic.buttonClick()
                    onFocusedFieldChange(field)
                },
                onDurationChange = onDurationChange,
                onDone = {
                    haptic.confirm()
                    onKeypadModeChange(false)
                },
                haptic = haptic
            )
        }
    }
}

@Composable
private fun WheelModeDisplay(
    hours: Int,
    minutes: Int,
    seconds: Int,
    onHoursChange: (Int) -> Unit,
    onMinutesChange: (Int) -> Unit,
    onSecondsChange: (Int) -> Unit,
    onTapField: (TimerUnitField) -> Unit,
    haptic: HapticManager
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Headers: "Hours"    "Minutes"    "Seconds"
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Hours",
                color = TextSecondary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.width(90.dp),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "Minutes",
                color = TextSecondary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.width(90.dp),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "Seconds",
                color = TextSecondary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.width(90.dp),
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 3-Column Wheels with Center Colons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Hours Wheel (0..99)
            WheelColumn(
                value = hours,
                range = 0..99,
                onValueChange = onHoursChange,
                onTap = { onTapField(TimerUnitField.HOURS) },
                haptic = haptic,
                modifier = Modifier.width(90.dp)
            )

            // Colon separator
            Text(
                text = ":",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 44.sp,
                fontWeight = FontWeight.Light,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            // Minutes Wheel (0..59)
            WheelColumn(
                value = minutes,
                range = 0..59,
                onValueChange = onMinutesChange,
                onTap = { onTapField(TimerUnitField.MINUTES) },
                haptic = haptic,
                modifier = Modifier.width(90.dp)
            )

            // Colon separator
            Text(
                text = ":",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 44.sp,
                fontWeight = FontWeight.Light,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            // Seconds Wheel (0..59)
            WheelColumn(
                value = seconds,
                range = 0..59,
                onValueChange = onSecondsChange,
                onTap = { onTapField(TimerUnitField.SECONDS) },
                haptic = haptic,
                modifier = Modifier.width(90.dp)
            )
        }
    }
}

/**
 * Interactive Single Wheel Column with fluid drag, momentum, and snapping.
 */
@Composable
private fun WheelColumn(
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    onTap: () -> Unit,
    haptic: HapticManager,
    modifier: Modifier = Modifier
) {
    val count = range.last - range.first + 1
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val accentColor = LocalAccentColor.current.primary
    val itemHeight = 60.dp
    val itemHeightPx = with(density) { itemHeight.toPx() }

    val animOffsetY = remember { Animatable(0f) }
    var currentVal by remember(value) { mutableIntStateOf(value) }
    var accumulatedDrag by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(value) {
        currentVal = value
    }

    Box(
        modifier = modifier
            .height(itemHeight * 3.2f)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onTap
            )
            .pointerInput(count) {
                var velocityTracker = VelocityTracker()
                detectVerticalDragGestures(
                    onDragStart = {
                        velocityTracker = VelocityTracker()
                        accumulatedDrag = 0f
                    },
                    onDragEnd = {
                        val velocityY = velocityTracker.calculateVelocity().y
                        coroutineScope.launch {
                            // If fling detected, fling several steps
                            if (abs(velocityY) > 800f) {
                                val steps = (-velocityY / 600f).roundToInt().coerceIn(-12, 12)
                                if (steps != 0) {
                                    val targetVal = (currentVal + steps + count * 10) % count
                                    currentVal = targetVal
                                    onValueChange(targetVal)
                                    haptic.sliderSnap()
                                }
                            }
                            animOffsetY.animateTo(
                                targetValue = 0f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            )
                            accumulatedDrag = 0f
                        }
                    },
                    onDragCancel = {
                        coroutineScope.launch {
                            animOffsetY.animateTo(0f)
                            accumulatedDrag = 0f
                        }
                    },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        velocityTracker.addPosition(change.uptimeMillis, change.position)
                        accumulatedDrag += dragAmount

                        coroutineScope.launch {
                            animOffsetY.snapTo(accumulatedDrag)
                        }

                        if (abs(accumulatedDrag) >= itemHeightPx * 0.5f) {
                            val step = if (accumulatedDrag > 0) -1 else 1
                            val nextVal = (currentVal + step + count) % count
                            currentVal = nextVal
                            onValueChange(nextVal)
                            haptic.sliderTick()
                            accumulatedDrag -= (-step) * itemHeightPx * 0.5f
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        val dragFraction = animOffsetY.value / itemHeightPx

        // Pre-compute items above and below
        val prevVal = (currentVal - 1 + count) % count
        val nextVal = (currentVal + 1) % count

        Column(
            modifier = Modifier.graphicsLayer {
                translationY = animOffsetY.value
            },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Previous Item (Above)
            Text(
                text = String.format("%02d", prevVal),
                color = Color(0xFF5A6270),
                fontSize = 36.sp,
                fontWeight = FontWeight.Normal,
                fontFamily = FontFamily.SansSerif,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .height(itemHeight)
                    .graphicsLayer {
                        alpha = 0.35f
                        scaleX = 0.82f
                        scaleY = 0.82f
                    }
            )

            // Current Selected Item (Center)
            Text(
                text = String.format("%02d", currentVal),
                color = accentColor,
                fontSize = 52.sp,
                fontWeight = FontWeight.Light,
                fontFamily = FontFamily.SansSerif,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .height(itemHeight)
                    .graphicsLayer {
                        alpha = 1.0f
                    }
            )

            // Next Item (Below)
            Text(
                text = String.format("%02d", nextVal),
                color = Color(0xFF5A6270),
                fontSize = 36.sp,
                fontWeight = FontWeight.Normal,
                fontFamily = FontFamily.SansSerif,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .height(itemHeight)
                    .graphicsLayer {
                        alpha = 0.35f
                        scaleX = 0.82f
                        scaleY = 0.82f
                    }
            )
        }
    }
}

/**
 * Keypad Mode: Displays the 3 units with a glowing rounded focus box
 * and an on-screen high-tech numeric keypad.
 */
@Composable
private fun KeypadModeDisplay(
    hours: Int,
    minutes: Int,
    seconds: Int,
    focusedField: TimerUnitField,
    onSelectField: (TimerUnitField) -> Unit,
    onDurationChange: (hours: Int, minutes: Int, seconds: Int) -> Unit,
    onDone: () -> Unit,
    haptic: HapticManager
) {
    // Digits input buffer for the active field
    var typedBuffer by remember { mutableStateOf("") }
    var currentField by remember(focusedField) { mutableStateOf(focusedField) }

    LaunchedEffect(focusedField) {
        currentField = focusedField
        typedBuffer = ""
    }

    fun handleDigitPress(digit: Int) {
        haptic.buttonClick()
        val newBuf = typedBuffer + digit.toString()

        when (currentField) {
            TimerUnitField.HOURS -> {
                val num = newBuf.toIntOrNull() ?: 0
                val clamped = num.coerceIn(0, 99)
                onDurationChange(clamped, minutes, seconds)
                if (newBuf.length >= 2) {
                    typedBuffer = ""
                    currentField = TimerUnitField.MINUTES
                    onSelectField(TimerUnitField.MINUTES)
                } else {
                    typedBuffer = newBuf
                }
            }
            TimerUnitField.MINUTES -> {
                val num = newBuf.toIntOrNull() ?: 0
                val clamped = num.coerceIn(0, 59)
                onDurationChange(hours, clamped, seconds)
                if (newBuf.length >= 2) {
                    typedBuffer = ""
                    currentField = TimerUnitField.SECONDS
                    onSelectField(TimerUnitField.SECONDS)
                } else {
                    typedBuffer = newBuf
                }
            }
            TimerUnitField.SECONDS -> {
                val num = newBuf.toIntOrNull() ?: 0
                val clamped = num.coerceIn(0, 59)
                onDurationChange(hours, minutes, clamped)
                if (newBuf.length >= 2) {
                    typedBuffer = ""
                } else {
                    typedBuffer = newBuf
                }
            }
        }
    }

    fun handleBackspace() {
        haptic.buttonClick()
        if (typedBuffer.isNotEmpty()) {
            val newBuf = typedBuffer.dropLast(1)
            typedBuffer = newBuf
            val num = newBuf.toIntOrNull() ?: 0
            when (currentField) {
                TimerUnitField.HOURS -> onDurationChange(num, minutes, seconds)
                TimerUnitField.MINUTES -> onDurationChange(hours, num, seconds)
                TimerUnitField.SECONDS -> onDurationChange(hours, minutes, num)
            }
        } else {
            // If empty, clear current field or shift back to previous
            when (currentField) {
                TimerUnitField.SECONDS -> {
                    onDurationChange(hours, minutes, 0)
                    currentField = TimerUnitField.MINUTES
                    onSelectField(TimerUnitField.MINUTES)
                }
                TimerUnitField.MINUTES -> {
                    onDurationChange(hours, 0, seconds)
                    currentField = TimerUnitField.HOURS
                    onSelectField(TimerUnitField.HOURS)
                }
                TimerUnitField.HOURS -> {
                    onDurationChange(0, minutes, seconds)
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Digits row with glowing focus border (as seen in video 00:05)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Hours Unit Box
            KeypadUnitBox(
                digits = String.format("%02d", hours),
                isFocused = currentField == TimerUnitField.HOURS,
                onClick = {
                    haptic.buttonClick()
                    typedBuffer = ""
                    currentField = TimerUnitField.HOURS
                    onSelectField(TimerUnitField.HOURS)
                }
            )

            Text(
                text = ":",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 44.sp,
                fontWeight = FontWeight.Light,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            // Minutes Unit Box
            KeypadUnitBox(
                digits = String.format("%02d", minutes),
                isFocused = currentField == TimerUnitField.MINUTES,
                onClick = {
                    haptic.buttonClick()
                    typedBuffer = ""
                    currentField = TimerUnitField.MINUTES
                    onSelectField(TimerUnitField.MINUTES)
                }
            )

            Text(
                text = ":",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 44.sp,
                fontWeight = FontWeight.Light,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            // Seconds Unit Box
            KeypadUnitBox(
                digits = String.format("%02d", seconds),
                isFocused = currentField == TimerUnitField.SECONDS,
                onClick = {
                    haptic.buttonClick()
                    typedBuffer = ""
                    currentField = TimerUnitField.SECONDS
                    onSelectField(TimerUnitField.SECONDS)
                }
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // On-screen Numeric Keypad (1..9, 0, backspace, done)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val keyRows = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("done", "0", "back")
            )

            keyRows.forEach { rowKeys ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    rowKeys.forEach { key ->
                        when (key) {
                            "back" -> {
                                KeypadCircleButton(
                                    onClick = { handleBackspace() }
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                                        contentDescription = "Backspace",
                                        tint = Color.White.copy(alpha = 0.9f),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            "done" -> {
                                val doneAccent = LocalAccentColor.current.primary
                                KeypadCircleButton(
                                    onClick = onDone,
                                    backgroundColor = doneAccent.copy(alpha = 0.22f),
                                    borderColor = doneAccent.copy(alpha = 0.55f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Done",
                                        tint = doneAccent,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            else -> {
                                val digit = key.toInt()
                                KeypadCircleButton(
                                    onClick = { handleDigitPress(digit) }
                                ) {
                                    Text(
                                        text = key,
                                        color = TextPrimary,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KeypadUnitBox(
    digits: String,
    isFocused: Boolean,
    onClick: () -> Unit
) {
    val accentColor = LocalAccentColor.current.primary
    val borderColor by animateColorAsState(
        targetValue = if (isFocused) accentColor else Color.Transparent,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "box_border"
    )
    val bgColor by animateColorAsState(
        targetValue = if (isFocused) accentColor.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.04f),
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "box_bg"
    )

    Box(
        modifier = Modifier
            .size(width = 86.dp, height = 64.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(2.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = digits,
            color = if (isFocused) accentColor else TextPrimary,
            fontSize = 48.sp,
            fontWeight = FontWeight.Light,
            fontFamily = FontFamily.SansSerif,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun KeypadCircleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = GlassSurface,
    borderColor: Color = GlassBorder,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .border(1.dp, borderColor, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
