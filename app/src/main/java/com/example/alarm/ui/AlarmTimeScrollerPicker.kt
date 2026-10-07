package com.example.alarm.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Keyboard
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
import com.example.util.HapticManager
import com.example.util.LocalHapticManager
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.math.abs
import kotlin.math.roundToInt

enum class TimePickerInputMode {
    SCROLLER,
    TYPE
}

enum class FocusedTimeField {
    HOUR,
    MINUTE
}

/**
 * Calculates human-readable "Next alarm in X hours Y minutes" string.
 */
fun calculateNextAlarmSubtitle(hour24: Int, minute: Int): String {
    val now = LocalDateTime.now()
    var target = now.withHour(hour24.coerceIn(0, 23))
        .withMinute(minute.coerceIn(0, 59))
        .withSecond(0)
        .withNano(0)

    if (!target.isAfter(now)) {
        target = target.plusDays(1)
    }

    val duration = Duration.between(now, target)
    val totalMinutes = duration.toMinutes()
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60

    return when {
        hours == 0L && minutes == 0L -> "Next alarm in less than 1 minute"
        hours == 0L -> "Next alarm in $minutes minute${if (minutes > 1) "s" else ""}"
        minutes == 0L -> "Next alarm in $hours hour${if (hours > 1) "s" else ""}"
        else -> "Next alarm in $hours hour${if (hours > 1) "s" else ""} $minutes minute${if (minutes > 1) "s" else ""}"
    }
}

/**
 * Dual-Mode Alarm Time Picker providing:
 * 1. 3D smooth vertical wheel scroller for Hours, Minutes, and AM/PM.
 * 2. Tactile keypad direct type mode.
 */
@Composable
fun AlarmTimePickerScroller(
    displayHour: Int, // 1..12 or 0..23
    minute: Int, // 0..59
    isPm: Boolean,
    is24Hour: Boolean = false,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit,
    onAmPmChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticManager.current
    val accentColor = LocalAccentColor.current.primary

    var inputMode by remember { mutableStateOf(TimePickerInputMode.SCROLLER) }
    var focusedField by remember { mutableStateOf(FocusedTimeField.HOUR) }

    // Convert displayHour and isPm to actual 24h for subtitle calculation
    val actualHour24 = if (is24Hour) {
        displayHour
    } else {
        when {
            !isPm && displayHour == 12 -> 0
            !isPm -> displayHour
            isPm && displayHour == 12 -> 12
            else -> displayHour + 12
        }
    }

    val subtitle = remember(actualHour24, minute) {
        calculateNextAlarmSubtitle(actualHour24, minute)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Next alarm subtitle banner (e.g. "Next alarm in 7 hours 8 minutes")
        Text(
            text = subtitle,
            color = TextSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Segmented Control Pill: [ 🔄 Scroller ]  [ ⌨️ Type ]
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(GlassSurface)
                .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Scroller mode button
            val isScroller = inputMode == TimePickerInputMode.SCROLLER
            val scrollerBg by animateColorAsState(
                if (isScroller) accentColor.copy(alpha = 0.28f) else Color.Transparent,
                label = "scrollerBg"
            )
            val scrollerBorder = if (isScroller) accentColor.copy(alpha = 0.6f) else Color.Transparent

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(scrollerBg)
                    .border(1.dp, scrollerBorder, RoundedCornerShape(16.dp))
                    .clickable {
                        haptic.buttonClick()
                        inputMode = TimePickerInputMode.SCROLLER
                    }
                    .padding(horizontal = 16.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AccessTime,
                    contentDescription = null,
                    tint = if (isScroller) Color.White else TextSecondary,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Scroller",
                    color = if (isScroller) Color.White else TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = if (isScroller) FontWeight.SemiBold else FontWeight.Normal
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Type mode button
            val isType = inputMode == TimePickerInputMode.TYPE
            val typeBg by animateColorAsState(
                if (isType) accentColor.copy(alpha = 0.28f) else Color.Transparent,
                label = "typeBg"
            )
            val typeBorder = if (isType) accentColor.copy(alpha = 0.6f) else Color.Transparent

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(typeBg)
                    .border(1.dp, typeBorder, RoundedCornerShape(16.dp))
                    .clickable {
                        haptic.buttonClick()
                        inputMode = TimePickerInputMode.TYPE
                    }
                    .padding(horizontal = 16.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Keyboard,
                    contentDescription = null,
                    tint = if (isType) Color.White else TextSecondary,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Type",
                    color = if (isType) Color.White else TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = if (isType) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Main Picker Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(GlassSurface)
                .border(1.dp, GlassBorderBright, RoundedCornerShape(26.dp))
                .padding(vertical = 12.dp, horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (inputMode == TimePickerInputMode.SCROLLER) {
                // SCROLLER WHEEL MODE
                AlarmScrollerColumns(
                    displayHour = displayHour,
                    minute = minute,
                    isPm = isPm,
                    is24Hour = is24Hour,
                    onHourChange = onHourChange,
                    onMinuteChange = onMinuteChange,
                    onAmPmChange = onAmPmChange,
                    onTapToType = { field ->
                        haptic.buttonClick()
                        focusedField = field
                        inputMode = TimePickerInputMode.TYPE
                    },
                    haptic = haptic
                )
            } else {
                // TYPE INPUT MODE
                AlarmTypeInputKeypad(
                    displayHour = displayHour,
                    minute = minute,
                    isPm = isPm,
                    is24Hour = is24Hour,
                    focusedField = focusedField,
                    onFocusedFieldChange = { focusedField = it },
                    onHourChange = onHourChange,
                    onMinuteChange = onMinuteChange,
                    onAmPmChange = onAmPmChange,
                    haptic = haptic
                )
            }
        }
    }
}

/**
 * 3-Column OxygenOS/iOS style 3D Wheel Picker for Alarm Time.
 */
@Composable
private fun AlarmScrollerColumns(
    displayHour: Int,
    minute: Int,
    isPm: Boolean,
    is24Hour: Boolean,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit,
    onAmPmChange: (Boolean) -> Unit,
    onTapToType: (FocusedTimeField) -> Unit,
    haptic: HapticManager
) {
    val accentColor = LocalAccentColor.current.primary
    val hourMin = if (is24Hour) 0 else 1
    val hourMax = if (is24Hour) 23 else 12

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp),
        contentAlignment = Alignment.Center
    ) {
        // Center horizontal highlight band
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(GlassSurfaceElevated)
                .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Hours Wheel Column
            AlarmWheelColumn(
                value = displayHour,
                minValue = hourMin,
                maxValue = hourMax,
                format = if (is24Hour) "%02d" else "%d",
                onValueChange = onHourChange,
                onTap = { onTapToType(FocusedTimeField.HOUR) },
                haptic = haptic,
                modifier = Modifier.width(76.dp)
            )

            // Colon separator
            Text(
                text = ":",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 42.sp,
                fontWeight = FontWeight.Light,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            // Minutes Wheel Column
            AlarmWheelColumn(
                value = minute,
                minValue = 0,
                maxValue = 59,
                format = "%02d",
                onValueChange = onMinuteChange,
                onTap = { onTapToType(FocusedTimeField.MINUTE) },
                haptic = haptic,
                modifier = Modifier.width(76.dp)
            )

            // AM / PM Column (only for 12-hour format)
            if (!is24Hour) {
                Spacer(modifier = Modifier.width(18.dp))
                AmPmWheelColumn(
                    isPm = isPm,
                    onAmPmChange = onAmPmChange,
                    haptic = haptic,
                    modifier = Modifier.width(64.dp)
                )
            }
        }

        // Top & Bottom gradient fades for 3D depth
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        0f to Color(0xFF101117).copy(alpha = 0.95f),
                        1f to Color.Transparent
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        1f to Color(0xFF101117).copy(alpha = 0.95f)
                    )
                )
        )
    }
}

/**
 * Individual Number Wheel Column with smooth vertical drag, fling, and cylindrical wrap.
 */
@Composable
private fun AlarmWheelColumn(
    value: Int,
    minValue: Int,
    maxValue: Int,
    format: String,
    onValueChange: (Int) -> Unit,
    onTap: () -> Unit,
    haptic: HapticManager,
    modifier: Modifier = Modifier
) {
    val count = maxValue - minValue + 1
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val itemHeight = 52.dp
    val itemHeightPx = with(density) { itemHeight.toPx() }

    val animOffsetY = remember { Animatable(0f) }
    var currentVal by remember(value) { mutableIntStateOf(value) }
    var accumulatedDrag by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(value) {
        currentVal = value
    }

    fun stepValue(current: Int, delta: Int): Int {
        val zeroIndex = current - minValue
        val newIndex = (zeroIndex + delta + count * 100) % count
        return minValue + newIndex
    }

    Box(
        modifier = modifier
            .height(itemHeight * 3.8f)
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
                            if (abs(velocityY) > 600f) {
                                val steps = (-velocityY / 500f).roundToInt().coerceIn(-8, 8)
                                if (steps != 0) {
                                    val nextVal = stepValue(currentVal, steps)
                                    currentVal = nextVal
                                    onValueChange(nextVal)
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
                            val nextVal = stepValue(currentVal, step)
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
        // Items above and below for 3D roll
        val valMinus2 = stepValue(currentVal, -2)
        val valMinus1 = stepValue(currentVal, -1)
        val valPlus1 = stepValue(currentVal, 1)
        val valPlus2 = stepValue(currentVal, 2)

        Column(
            modifier = Modifier.graphicsLayer {
                translationY = animOffsetY.value
            },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Far item above (-2)
            Text(
                text = String.format(format, valMinus2),
                color = TextTertiary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Light,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .height(itemHeight * 0.7f)
                    .graphicsLayer {
                        alpha = 0.22f
                        scaleX = 0.75f
                        scaleY = 0.75f
                    }
            )

            // Item directly above (-1)
            Text(
                text = String.format(format, valMinus1),
                color = TextSecondary,
                fontSize = 32.sp,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .height(itemHeight)
                    .graphicsLayer {
                        alpha = 0.45f
                        scaleX = 0.84f
                        scaleY = 0.84f
                    }
            )

            // Center Selected Item (0)
            Text(
                text = String.format(format, currentVal),
                color = Color.White,
                fontSize = 44.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .height(itemHeight)
                    .graphicsLayer {
                        scaleX = 1.06f
                        scaleY = 1.06f
                    }
            )

            // Item directly below (+1)
            Text(
                text = String.format(format, valPlus1),
                color = TextSecondary,
                fontSize = 32.sp,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .height(itemHeight)
                    .graphicsLayer {
                        alpha = 0.45f
                        scaleX = 0.84f
                        scaleY = 0.84f
                    }
            )

            // Far item below (+2)
            Text(
                text = String.format(format, valPlus2),
                color = TextTertiary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Light,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .height(itemHeight * 0.7f)
                    .graphicsLayer {
                        alpha = 0.22f
                        scaleX = 0.75f
                        scaleY = 0.75f
                    }
            )
        }
    }
}

/**
 * AM / PM Wheel Column with smooth vertical drag / click toggle.
 */
@Composable
private fun AmPmWheelColumn(
    isPm: Boolean,
    onAmPmChange: (Boolean) -> Unit,
    haptic: HapticManager,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val itemHeight = 52.dp
    val itemHeightPx = with(density) { itemHeight.toPx() }

    val animOffsetY = remember { Animatable(0f) }
    var accumulatedDrag by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .height(itemHeight * 3.2f)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    haptic.buttonClick()
                    onAmPmChange(!isPm)
                }
            )
            .pointerInput(isPm) {
                detectVerticalDragGestures(
                    onDragStart = { accumulatedDrag = 0f },
                    onDragEnd = {
                        coroutineScope.launch {
                            animOffsetY.animateTo(0f, spring(dampingRatio = 0.8f))
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
                        accumulatedDrag += dragAmount

                        coroutineScope.launch {
                            animOffsetY.snapTo(accumulatedDrag)
                        }

                        if (abs(accumulatedDrag) >= itemHeightPx * 0.4f) {
                            onAmPmChange(!isPm)
                            haptic.sliderTick()
                            accumulatedDrag = 0f
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.graphicsLayer {
                translationY = animOffsetY.value
            },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Other option (above)
            Text(
                text = if (isPm) "AM" else "PM",
                color = TextSecondary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Normal,
                modifier = Modifier
                    .height(itemHeight)
                    .graphicsLayer {
                        alpha = 0.4f
                        scaleX = 0.85f
                        scaleY = 0.85f
                    }
            )

            // Current Selected (center)
            Text(
                text = if (isPm) "PM" else "AM",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.height(itemHeight)
            )

            // Other option (below)
            Text(
                text = if (isPm) "AM" else "PM",
                color = TextSecondary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Normal,
                modifier = Modifier
                    .height(itemHeight)
                    .graphicsLayer {
                        alpha = 0.4f
                        scaleX = 0.85f
                        scaleY = 0.85f
                    }
            )
        }
    }
}

/**
 * Direct Type Input Mode with interactive hour/minute boxes and tactile keypad.
 */
@Composable
private fun AlarmTypeInputKeypad(
    displayHour: Int,
    minute: Int,
    isPm: Boolean,
    is24Hour: Boolean,
    focusedField: FocusedTimeField,
    onFocusedFieldChange: (FocusedTimeField) -> Unit,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit,
    onAmPmChange: (Boolean) -> Unit,
    haptic: HapticManager
) {
    val accentColor = LocalAccentColor.current.primary

    // Buffer states for typing digits
    var hourBuffer by remember { mutableStateOf("") }
    var minuteBuffer by remember { mutableStateOf("") }

    LaunchedEffect(displayHour) {
        if (hourBuffer.isEmpty()) {
            // keep synced
        }
    }
    LaunchedEffect(minute) {
        if (minuteBuffer.isEmpty()) {
            // keep synced
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Large Interactive Time Display
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Hour Pill
            val isHourFocused = focusedField == FocusedTimeField.HOUR
            val hourBorder = if (isHourFocused) accentColor else GlassBorder
            val hourBg = if (isHourFocused) accentColor.copy(alpha = 0.18f) else GlassSurfaceElevated

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(hourBg)
                    .border(if (isHourFocused) 2.dp else 1.dp, hourBorder, RoundedCornerShape(18.dp))
                    .clickable {
                        haptic.buttonClick()
                        onFocusedFieldChange(FocusedTimeField.HOUR)
                        hourBuffer = ""
                    }
                    .padding(horizontal = 22.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (hourBuffer.isNotEmpty()) hourBuffer else String.format(if (is24Hour) "%02d" else "%d", displayHour),
                    color = Color.White,
                    fontSize = 46.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.SansSerif
                )
            }

            Text(
                text = ":",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 44.sp,
                fontWeight = FontWeight.Light,
                modifier = Modifier.padding(horizontal = 10.dp)
            )

            // Minute Pill
            val isMinuteFocused = focusedField == FocusedTimeField.MINUTE
            val minuteBorder = if (isMinuteFocused) accentColor else GlassBorder
            val minuteBg = if (isMinuteFocused) accentColor.copy(alpha = 0.18f) else GlassSurfaceElevated

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(minuteBg)
                    .border(if (isMinuteFocused) 2.dp else 1.dp, minuteBorder, RoundedCornerShape(18.dp))
                    .clickable {
                        haptic.buttonClick()
                        onFocusedFieldChange(FocusedTimeField.MINUTE)
                        minuteBuffer = ""
                    }
                    .padding(horizontal = 22.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (minuteBuffer.isNotEmpty()) minuteBuffer.padStart(2, '0') else String.format("%02d", minute),
                    color = Color.White,
                    fontSize = 46.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.SansSerif
                )
            }

            // AM/PM Toggle Pill
            if (!is24Hour) {
                Spacer(modifier = Modifier.width(14.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(accentColor.copy(alpha = 0.22f))
                        .border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                        .clickable {
                            haptic.buttonClick()
                            onAmPmChange(!isPm)
                        }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isPm) "PM" else "AM",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Direct Numeric Keypad
        val maxHour = if (is24Hour) 23 else 12

        fun onDigitClick(digit: Int) {
            haptic.buttonClick()
            if (focusedField == FocusedTimeField.HOUR) {
                val newBuffer = (hourBuffer + digit).takeLast(2)
                hourBuffer = newBuffer
                val parsed = newBuffer.toIntOrNull()
                if (parsed != null) {
                    if (parsed in 0..maxHour) {
                        onHourChange(if (!is24Hour && parsed == 0) 12 else parsed)
                    }
                    // Auto advance to minute if 2 digits or number > 1 in 12h
                    if (newBuffer.length == 2 || (!is24Hour && parsed > 1)) {
                        onFocusedFieldChange(FocusedTimeField.MINUTE)
                        minuteBuffer = ""
                    }
                }
            } else {
                val newBuffer = (minuteBuffer + digit).takeLast(2)
                minuteBuffer = newBuffer
                val parsed = newBuffer.toIntOrNull()
                if (parsed != null && parsed in 0..59) {
                    onMinuteChange(parsed)
                }
            }
        }

        fun onBackspaceClick() {
            haptic.buttonClick()
            if (focusedField == FocusedTimeField.MINUTE) {
                if (minuteBuffer.isNotEmpty()) {
                    minuteBuffer = minuteBuffer.dropLast(1)
                    val parsed = minuteBuffer.toIntOrNull() ?: 0
                    onMinuteChange(parsed)
                } else {
                    onFocusedFieldChange(FocusedTimeField.HOUR)
                }
            } else {
                if (hourBuffer.isNotEmpty()) {
                    hourBuffer = hourBuffer.dropLast(1)
                    val parsed = hourBuffer.toIntOrNull() ?: 1
                    onHourChange(parsed)
                }
            }
        }

        val rows = listOf(
            listOf(1, 2, 3),
            listOf(4, 5, 6),
            listOf(7, 8, 9)
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            rows.forEach { rowDigits ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowDigits.forEach { digit ->
                        KeypadButton(
                            text = digit.toString(),
                            onClick = { onDigitClick(digit) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Bottom row: [AM/PM or Clear], [0], [Backspace]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Bottom left: AM/PM switch
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(GlassSurface)
                        .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                        .clickable {
                            haptic.buttonClick()
                            onAmPmChange(!isPm)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isPm) "AM" else "PM",
                        color = accentColor,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // 0 digit
                KeypadButton(
                    text = "0",
                    onClick = { onDigitClick(0) },
                    modifier = Modifier.weight(1f)
                )

                // Backspace button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(GlassSurface)
                        .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                        .clickable { onBackspaceClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                        contentDescription = "Backspace",
                        tint = TextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun KeypadButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(GlassSurface)
            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
