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
import com.example.ui.theme.InterfaceStyle
import com.example.ui.theme.LocalAccentColor
import com.example.ui.theme.LocalInterfaceStyle
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
        // Next alarm countdown badge
        Box(
            modifier = Modifier
                .padding(bottom = 12.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(accentColor.copy(alpha = 0.12f))
                .border(1.dp, accentColor.copy(alpha = 0.30f), RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AccessTime,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.90f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

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

        val currentStyle = LocalInterfaceStyle.current
        val pickerCardShape = RoundedCornerShape(26.dp)

        // Main Picker Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(pickerCardShape)
                .then(
                    when (currentStyle) {
                        InterfaceStyle.PURE_LIQUID_GLASS -> Modifier
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.16f),
                                        accentColor.copy(alpha = 0.08f),
                                        Color(0xFF10121C).copy(alpha = 0.85f)
                                    )
                                )
                            )
                            .border(
                                1.2.dp,
                                Brush.verticalGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.70f),
                                        accentColor.copy(alpha = 0.40f)
                                    )
                                ),
                                pickerCardShape
                            )
                        InterfaceStyle.MINIMAL -> Modifier
                            .background(Color(0xFF1C1C1E))
                            .border(1.dp, Color(0xFF2C2C2E), pickerCardShape)
                        InterfaceStyle.MIX_LIQUID -> Modifier
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color(0xFF20222A).copy(alpha = 0.94f),
                                        Color(0xFF14151C).copy(alpha = 0.96f)
                                    )
                                )
                            )
                            .border(1.dp, GlassBorderBright, pickerCardShape)
                    }
                )
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
 * Perfectly aligned horizontal selection band with sub-pixel vertical centering across
 * Hours, Colon separator, Minutes, and AM/PM.
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
    val currentStyle = LocalInterfaceStyle.current
    val density = LocalDensity.current

    val hourMin = if (is24Hour) 0 else 1
    val hourMax = if (is24Hour) 23 else 12

    val itemHeight = 56.dp
    val viewportHeight = 200.dp
    val bandShape = RoundedCornerShape(16.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(viewportHeight),
        contentAlignment = Alignment.Center
    ) {
        // Center horizontal highlight band (Adapts across Pure Minimal, Pure Liquid Glass, Mix Liquid)
        // Its height matches itemHeight exactly, guaranteeing dead-center alignment.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .clip(bandShape)
                .then(
                    when (currentStyle) {
                        InterfaceStyle.PURE_LIQUID_GLASS -> Modifier
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.22f),
                                        accentColor.copy(alpha = 0.15f),
                                        Color.White.copy(alpha = 0.05f)
                                    )
                                )
                            )
                            .border(
                                1.2.dp,
                                Brush.verticalGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.85f),
                                        accentColor.copy(alpha = 0.50f)
                                    )
                                ),
                                bandShape
                            )
                        InterfaceStyle.MINIMAL -> Modifier
                            .background(Color(0xFF2C2C2E))
                            .border(1.dp, Color(0xFF3A3A3C), bandShape)
                        InterfaceStyle.MIX_LIQUID -> Modifier
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color(0xFF282A34),
                                        Color(0xFF191B22)
                                    )
                                )
                            )
                            .border(1.dp, accentColor.copy(alpha = 0.45f), bandShape)
                    }
                )
        )

        // All columns share the exact same row with vertical centering, guaranteeing perfect alignment
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Hours Wheel Column
            TimeWheelColumn(
                value = displayHour,
                minValue = hourMin,
                maxValue = hourMax,
                formatValue = { if (is24Hour) String.format("%02d", it) else it.toString() },
                onValueChange = onHourChange,
                onTap = { onTapToType(FocusedTimeField.HOUR) },
                haptic = haptic,
                itemHeight = itemHeight,
                viewportHeight = viewportHeight,
                modifier = Modifier.width(if (is24Hour) 80.dp else 72.dp)
            )

            // Colon separator (locked inside a Box of exact same height itemHeight)
            Box(
                modifier = Modifier
                    .width(if (is24Hour) 22.dp else 18.dp)
                    .height(itemHeight),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = ":",
                    color = Color.White.copy(alpha = 0.88f),
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Light,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.graphicsLayer {
                        // Optical baseline centering so colon dots align symmetrically with digit cap-height
                        translationY = -2f * density.density
                    }
                )
            }

            // Minutes Wheel Column
            TimeWheelColumn(
                value = minute,
                minValue = 0,
                maxValue = 59,
                formatValue = { String.format("%02d", it) },
                onValueChange = onMinuteChange,
                onTap = { onTapToType(FocusedTimeField.MINUTE) },
                haptic = haptic,
                itemHeight = itemHeight,
                viewportHeight = viewportHeight,
                modifier = Modifier.width(if (is24Hour) 80.dp else 72.dp)
            )

            // AM / PM Column (only for 12-hour format)
            if (!is24Hour) {
                Spacer(modifier = Modifier.width(16.dp))
                TimeWheelColumn(
                    value = if (isPm) 1 else 0,
                    minValue = 0,
                    maxValue = 1,
                    formatValue = { if (it == 1) "PM" else "AM" },
                    onValueChange = { onAmPmChange(it == 1) },
                    onTap = { onAmPmChange(!isPm) },
                    isAmPm = true,
                    haptic = haptic,
                    itemHeight = itemHeight,
                    viewportHeight = viewportHeight,
                    modifier = Modifier.width(68.dp)
                )
            }
        }

        // Top & Bottom gradient fades for authentic 3D depth and smooth entry/exit
        val fadeColor = when (currentStyle) {
            InterfaceStyle.MINIMAL -> Color(0xFF1C1C1E)
            InterfaceStyle.PURE_LIQUID_GLASS -> Color(0xFF10121C).copy(alpha = 0.95f)
            InterfaceStyle.MIX_LIQUID -> Color(0xFF14151C).copy(alpha = 0.95f)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        0f to fadeColor,
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
                        1f to fadeColor
                    )
                )
        )
    }
}

/**
 * Universal High-Precision Wheel Column providing:
 * 1. 1:1 true continuous sliding with zero skips, jumps, or stutter.
 * 2. High-speed fling momentum that spins freely and softly settles on an exact integer.
 * 3. Tactile haptic ticks on every single number that glides across the center.
 * 4. Realistic 3D cylindrical perspective (rotationX, scale, opacity fading).
 * 5. Tap-to-center for adjacent visible numbers.
 */
@Composable
private fun TimeWheelColumn(
    value: Int,
    minValue: Int,
    maxValue: Int,
    onValueChange: (Int) -> Unit,
    formatValue: (Int) -> String,
    haptic: HapticManager,
    modifier: Modifier = Modifier,
    isAmPm: Boolean = false,
    itemHeight: Dp = 56.dp,
    viewportHeight: Dp = 200.dp,
    onTap: (() -> Unit)? = null
) {
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val itemHeightPx = with(density) { itemHeight.toPx() }
    val rangeCount = maxValue - minValue + 1

    fun wrapVal(raw: Int): Int {
        val zeroBased = raw - minValue
        val mod = ((zeroBased % rangeCount) + rangeCount) % rangeCount
        return minValue + mod
    }

    // Continuous pixel offset from base anchor
    val scrollOffset = remember { Animatable(0f) }
    var baseValue by remember(value) { mutableIntStateOf(value) }
    var isDragging by remember { mutableStateOf(false) }
    var lastReportedValue by remember(value) { mutableIntStateOf(value) }

    LaunchedEffect(value) {
        if (!isDragging && value != baseValue) {
            baseValue = value
            scrollOffset.snapTo(0f)
            lastReportedValue = value
        }
    }

    val currentOffset = scrollOffset.value
    // Dragging DOWN (positive offset) means smaller/previous numbers come down to center
    val totalItemsShifted = -currentOffset / itemHeightPx
    val centerStep = totalItemsShifted.roundToInt()
    val currentCenterVal = wrapVal(baseValue + centerStep)

    // Sub-item pixel offset from the current integer center
    val pixelOffsetFromCenter = currentOffset + (centerStep * itemHeightPx)

    // Trigger crisp haptic tick whenever a number passes the center threshold
    LaunchedEffect(currentCenterVal) {
        if (currentCenterVal != lastReportedValue) {
            lastReportedValue = currentCenterVal
            haptic.sliderTick()
            onValueChange(currentCenterVal)
        }
    }

    Box(
        modifier = modifier
            .height(viewportHeight)
            .pointerInput(rangeCount) {
                var velocityTracker = VelocityTracker()
                detectVerticalDragGestures(
                    onDragStart = {
                        isDragging = true
                        velocityTracker = VelocityTracker()
                    },
                    onDragEnd = {
                        val velocityY = velocityTracker.calculateVelocity().y
                        isDragging = false
                        coroutineScope.launch {
                            // Authentic inertia / fling momentum
                            val flingPixels = if (abs(velocityY) > 300f) {
                                (velocityY * 0.22f).coerceIn(-itemHeightPx * 12f, itemHeightPx * 12f)
                            } else {
                                0f
                            }
                            val projectedOffset = scrollOffset.value + flingPixels
                            val projectedStep = (-projectedOffset / itemHeightPx).roundToInt()
                            val finalSnapTarget = -projectedStep * itemHeightPx

                            scrollOffset.animateTo(
                                targetValue = finalSnapTarget,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            )

                            val finalVal = wrapVal(baseValue + projectedStep)
                            baseValue = finalVal
                            scrollOffset.snapTo(0f)
                            onValueChange(finalVal)
                            haptic.sliderSnap()
                        }
                    },
                    onDragCancel = {
                        isDragging = false
                        coroutineScope.launch {
                            val currentStepInt = (-scrollOffset.value / itemHeightPx).roundToInt()
                            val finalSnapTarget = -currentStepInt * itemHeightPx
                            scrollOffset.animateTo(
                                targetValue = finalSnapTarget,
                                animationSpec = spring(dampingRatio = 0.8f)
                            )
                            val finalVal = wrapVal(baseValue + currentStepInt)
                            baseValue = finalVal
                            scrollOffset.snapTo(0f)
                            onValueChange(finalVal)
                        }
                    },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        velocityTracker.addPosition(change.uptimeMillis, change.position)
                        coroutineScope.launch {
                            scrollOffset.snapTo(scrollOffset.value + dragAmount)
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Render 5 items (-2, -1, 0, +1, +2) with continuous smooth 3D cylindrical roll
        for (k in -2..2) {
            val itemVal = wrapVal(currentCenterVal + k)
            val yPos = (k * itemHeightPx) + pixelOffsetFromCenter
            val distFraction = (yPos / (itemHeightPx * 2.2f)).coerceIn(-1.2f, 1.2f)
            val absDist = abs(distFraction)

            val rotX = (-distFraction * 36f).coerceIn(-60f, 60f)
            val scale = (1.05f - (absDist * 0.22f)).coerceIn(0.72f, 1.05f)
            val alpha = (1.0f - (absDist * 0.58f)).coerceIn(0.18f, 1.0f)
            val isClosest = abs(yPos) < itemHeightPx * 0.45f

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(itemHeight)
                    .graphicsLayer {
                        translationY = yPos
                        rotationX = rotX
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                        cameraDistance = 14f * density.density
                    }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (!isClosest) {
                            // Tapping an adjacent item rolls it smoothly to the center
                            haptic.buttonClick()
                            coroutineScope.launch {
                                val targetDelta = -k * itemHeightPx
                                scrollOffset.animateTo(
                                    targetValue = scrollOffset.value + targetDelta,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioLowBouncy,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                )
                                val newCenterVal = wrapVal(baseValue + centerStep + k)
                                baseValue = newCenterVal
                                scrollOffset.snapTo(0f)
                                onValueChange(newCenterVal)
                            }
                        } else if (onTap != null) {
                            onTap()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = formatValue(itemVal),
                    color = if (isClosest) Color.White else TextSecondary,
                    fontSize = if (isAmPm) {
                        if (isClosest) 23.sp else 19.sp
                    } else {
                        if (isClosest) 40.sp else 31.sp
                    },
                    fontWeight = if (isClosest) FontWeight.SemiBold else FontWeight.Normal,
                    fontFamily = FontFamily.SansSerif,
                    textAlign = TextAlign.Center
                )
            }
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
