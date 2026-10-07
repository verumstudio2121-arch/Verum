package com.example.settings

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DeepBlack
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassBorderBright
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.GlassSurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.util.LocalHapticManager
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Converts HSV values to Compose Color.
 * Hue: 0..360, Saturation: 0..1, Value/Brightness: 0..1
 */
fun hsvToColor(hue: Float, saturation: Float, value: Float): Color {
    val hsv = floatArrayOf(hue.coerceIn(0f, 360f), saturation.coerceIn(0f, 1f), value.coerceIn(0f, 1f))
    return Color(AndroidColor.HSVToColor(hsv))
}

/**
 * Converts a Compose Color to hex string format "#RRGGBB".
 */
fun Color.toHexRgb(): String {
    val r = (red * 255).toInt().coerceIn(0, 255)
    val g = (green * 255).toInt().coerceIn(0, 255)
    val b = (blue * 255).toInt().coerceIn(0, 255)
    return String.format("#%02X%02X%02X", r, g, b)
}

/**
 * Extracts HSV components from an existing Color.
 */
fun colorToHsv(color: Color): FloatArray {
    val hsv = FloatArray(3)
    val r = (color.red * 255).toInt()
    val g = (color.green * 255).toInt()
    val b = (color.blue * 255).toInt()
    AndroidColor.RGBToHSV(r, g, b, hsv)
    return hsv
}

/**
 * Premium Liquid Glass Full Spectrum Color Wheel Dialog / Bottom Sheet
 * Allows picking any color in the 360° spectrum with Hue Wheel, Saturation & Brightness Sliders,
 * and quick-pick curated neon/pastel swatches.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorWheelBottomSheet(
    initialColor: Color,
    onColorSelected: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticManager.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val initialHsv = remember(initialColor) { colorToHsv(initialColor) }
    var hue by remember { mutableFloatStateOf(initialHsv[0]) }
    var saturation by remember { mutableFloatStateOf(initialHsv[1].coerceAtLeast(0.2f)) }
    var brightness by remember { mutableFloatStateOf(initialHsv[2].coerceAtLeast(0.2f)) }

    val activeSelectedColor = remember(hue, saturation, brightness) {
        hsvToColor(hue, saturation, brightness)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DeepBlack,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(42.dp)
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
                .padding(bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Row: Title & Close
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Custom Accent Wheel",
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Choose any custom color for your liquid clock",
                        color = TextTertiary,
                        fontSize = 12.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(GlassSurface)
                        .border(1.dp, GlassBorder, CircleShape)
                        .clickable {
                            haptic.buttonClick()
                            onDismiss()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // The Interactive Continuous Color Wheel
            Box(
                modifier = Modifier
                    .size(230.dp),
                contentAlignment = Alignment.Center
            ) {
                ContinuousColorWheel(
                    hue = hue,
                    saturation = saturation,
                    onHueSatChange = { newHue, newSat ->
                        hue = newHue
                        saturation = newSat
                    },
                    modifier = Modifier.size(230.dp)
                )

                // Center Preview Orb showing active color with liquid glass ring
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(activeSelectedColor)
                        .border(2.5.dp, Color.White, CircleShape)
                        .border(4.5.dp, Color.White.copy(alpha = 0.25f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Current Hex & RGB readout capsule
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(GlassSurfaceElevated)
                    .border(1.dp, GlassBorderBright, CircleShape)
                    .padding(horizontal = 16.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(activeSelectedColor)
                        .border(1.dp, Color.White, CircleShape)
                )
                Text(
                    text = activeSelectedColor.toHexRgb(),
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "H:${hue.toInt()}° S:${(saturation * 100).toInt()}% B:${(brightness * 100).toInt()}%",
                    color = TextTertiary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Brightness / Value Slider
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Brightness",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${(brightness * 100).toInt()}%",
                        color = TextTertiary,
                        fontSize = 12.sp
                    )
                }
                Slider(
                    value = brightness,
                    onValueChange = { brightness = it },
                    valueRange = 0.15f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = activeSelectedColor,
                        inactiveTrackColor = GlassSurface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Quick Preset Highlights (Popular neon and vibrant colors)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val popularHues = listOf(
                    Color(0xFF38BDF8), // Electric Cyan
                    Color(0xFF818CF8), // Indigo
                    Color(0xFFA78BFA), // Lavender
                    Color(0xFFF472B6), // Hot Pink
                    Color(0xFFFB7185), // Coral Rose
                    Color(0xFFFF9F0A), // Neon Orange
                    Color(0xFFFFD60A), // Cyber Yellow
                    Color(0xFF34D399), // Emerald
                    Color(0xFF2DD4BF)  // Mint
                )

                popularHues.forEach { swatchColor ->
                    val isNear = swatchColor.toHexRgb() == activeSelectedColor.toHexRgb()
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(swatchColor)
                            .border(
                                width = if (isNear) 2.5.dp else 1.dp,
                                color = if (isNear) Color.White else Color.White.copy(alpha = 0.3f),
                                shape = CircleShape
                            )
                            .clickable {
                                haptic.buttonClick()
                                val hsv = colorToHsv(swatchColor)
                                hue = hsv[0]
                                saturation = hsv[1]
                                brightness = hsv[2]
                            }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons: Done / Apply
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Cancel
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(GlassSurface)
                        .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                        .clickable {
                            haptic.buttonClick()
                            onDismiss()
                        }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Cancel",
                        color = TextSecondary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Apply Accent Color
                Box(
                    modifier = Modifier
                        .weight(1.5f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(activeSelectedColor)
                        .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                        .clickable {
                            haptic.confirm()
                            onColorSelected(activeSelectedColor)
                            onDismiss()
                        }
                        .padding(vertical = 14.dp)
                        .testTag("apply_color_wheel_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Apply Color",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Continuous 360° circular hue & saturation wheel drawn using native Canvas arcs.
 */
@Composable
private fun ContinuousColorWheel(
    hue: Float,
    saturation: Float,
    onHueSatChange: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticManager.current

    // Generate circular spectrum segments
    val sweepColors = remember {
        listOf(
            Color(0xFFFF0000), // 0° Red
            Color(0xFFFFFF00), // 60° Yellow
            Color(0xFF00FF00), // 120° Green
            Color(0xFF00FFFF), // 180° Cyan
            Color(0xFF0000FF), // 240° Blue
            Color(0xFFFF00FF), // 300° Magenta
            Color(0xFFFF0000)  // 360° Red
        )
    }

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val dx = offset.x - center.x
                    val dy = offset.y - center.y
                    val radius = min(size.width, size.height) / 2f

                    var angle = (atan2(dy, dx) * 180f / PI).toFloat()
                    if (angle < 0f) angle += 360f

                    val dist = sqrt(dx * dx + dy * dy)
                    val sat = (dist / radius).coerceIn(0.1f, 1f)

                    haptic.clockTick()
                    onHueSatChange(angle, sat)
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    change.consume()
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val dx = change.position.x - center.x
                    val dy = change.position.y - center.y
                    val radius = min(size.width, size.height) / 2f

                    var angle = (atan2(dy, dx) * 180f / PI).toFloat()
                    if (angle < 0f) angle += 360f

                    val dist = sqrt(dx * dx + dy * dy)
                    val sat = (dist / radius).coerceIn(0.1f, 1f)

                    onHueSatChange(angle, sat)
                }
            }
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val outerRadius = min(size.width, size.height) / 2f
            val ringWidth = outerRadius * 0.42f
            val ringRadius = outerRadius - ringWidth / 2f

            // Draw full spectrum circular ring
            drawCircle(
                brush = Brush.sweepGradient(sweepColors, center),
                radius = ringRadius,
                center = center,
                style = Stroke(width = ringWidth, cap = StrokeCap.Round)
            )

            // Outer delicate glass border
            drawCircle(
                color = Color.White.copy(alpha = 0.35f),
                radius = outerRadius,
                center = center,
                style = Stroke(width = 1.5.dp.toPx())
            )

            // Inner glass rim
            drawCircle(
                color = Color.White.copy(alpha = 0.25f),
                radius = outerRadius - ringWidth,
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )

            // Indicator position on the ring
            val angleRad = (hue * PI / 180.0).toFloat()
            val indicatorRadius = ringRadius
            val indicatorX = center.x + indicatorRadius * cos(angleRad)
            val indicatorY = center.y + indicatorRadius * sin(angleRad)

            // Outer glow
            drawCircle(
                color = Color.White.copy(alpha = 0.45f),
                radius = 16.dp.toPx(),
                center = Offset(indicatorX, indicatorY)
            )

            // Indicator white knob
            drawCircle(
                color = Color.White,
                radius = 12.dp.toPx(),
                center = Offset(indicatorX, indicatorY)
            )

            // Inner filled dot with currently selected hue
            drawCircle(
                color = hsvToColor(hue, 1f, 1f),
                radius = 8.dp.toPx(),
                center = Offset(indicatorX, indicatorY)
            )
        }
    }
}
