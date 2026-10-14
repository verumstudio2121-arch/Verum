package com.example.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Timer
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
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.LocalHapticManager
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Reusable Modifier extension for the Liquid Glass glassmorphism effect.
 *
 * Blends:
 * 1. Shape clipping ([shape])
 * 2. Background blur via RenderEffect on Android 12+ ([blurRadius])
 * 3. Subtle semi-transparent white fill ([backgroundColor])
 * 4. Specular liquid reflection gradient sheen ([specularHighlight])
 * 5. Refined thin translucent border ([borderWidth], [borderColor] or [borderBrush])
 */
fun Modifier.liquidGlass(
    shape: Shape = RoundedCornerShape(24.dp),
    backgroundColor: Color = GlassSurface,
    borderColor: Color = GlassBorder,
    borderWidth: Dp = 1.dp,
    blurRadius: Dp = 16.dp,
    specularHighlight: Boolean = true,
    specularAlpha: Float = 0.12f
): Modifier = this
    .clip(shape)
    .then(
        if (blurRadius > 0.dp) {
            Modifier.blur(
                radius = blurRadius,
                edgeTreatment = BlurredEdgeTreatment(shape)
            )
        } else {
            Modifier
        }
    )
    .background(backgroundColor, shape)
    .then(
        if (specularHighlight) {
            Modifier.drawBehind {
                drawRect(
                    brush = Brush.verticalGradient(
                        0.0f to Color.White.copy(alpha = specularAlpha),
                        0.35f to Color.White.copy(alpha = specularAlpha * 0.35f),
                        1.0f to Color.Transparent
                    )
                )
            }
        } else {
            Modifier
        }
    )
    .border(borderWidth, borderColor, shape)

/**
 * Liquid Glass modifier overload accepting a custom [borderBrush] for dynamic light reflections.
 */
fun Modifier.liquidGlass(
    shape: Shape = RoundedCornerShape(24.dp),
    backgroundColor: Color = GlassSurface,
    borderBrush: Brush,
    borderWidth: Dp = 1.dp,
    blurRadius: Dp = 16.dp,
    specularHighlight: Boolean = true,
    specularAlpha: Float = 0.12f
): Modifier = this
    .clip(shape)
    .then(
        if (blurRadius > 0.dp) {
            Modifier.blur(
                radius = blurRadius,
                edgeTreatment = BlurredEdgeTreatment(shape)
            )
        } else {
            Modifier
        }
    )
    .background(backgroundColor, shape)
    .then(
        if (specularHighlight) {
            Modifier.drawBehind {
                drawRect(
                    brush = Brush.verticalGradient(
                        0.0f to Color.White.copy(alpha = specularAlpha),
                        0.35f to Color.White.copy(alpha = specularAlpha * 0.35f),
                        1.0f to Color.Transparent
                    )
                )
            }
        } else {
            Modifier
        }
    )
    .border(borderWidth, borderBrush, shape)

/**
 * Convenience modifier for an elevated/prominent Liquid Glass surface
 * (e.g., floating panels, active cards, bottom sheets).
 */
fun Modifier.liquidGlassElevated(
    shape: Shape = RoundedCornerShape(28.dp),
    backgroundColor: Color = GlassSurfaceElevated,
    borderColor: Color = GlassBorderBright,
    borderWidth: Dp = 1.dp,
    blurRadius: Dp = 20.dp,
    specularHighlight: Boolean = true
): Modifier = liquidGlass(
    shape = shape,
    backgroundColor = backgroundColor,
    borderColor = borderColor,
    borderWidth = borderWidth,
    blurRadius = blurRadius,
    specularHighlight = specularHighlight,
    specularAlpha = 0.18f
)

/**
 * Semantic alias for [liquidGlass] matching the glassmorphism aesthetic.
 */
fun Modifier.glassmorphism(
    shape: Shape = RoundedCornerShape(24.dp),
    backgroundColor: Color = GlassSurface,
    borderColor: Color = GlassBorder,
    borderWidth: Dp = 1.dp,
    blurRadius: Dp = 16.dp,
    specularHighlight: Boolean = true
): Modifier = liquidGlass(
    shape = shape,
    backgroundColor = backgroundColor,
    borderColor = borderColor,
    borderWidth = borderWidth,
    blurRadius = blurRadius,
    specularHighlight = specularHighlight
)

/**
 * Container composable for a Liquid Glass surface with an isolated blurred background layer,
 * ensuring foreground children (such as text, numbers, and icons) remain ultra-sharp and legible.
 */
@Composable
fun LiquidGlassBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    backgroundColor: Color = GlassSurface,
    borderColor: Color = GlassBorder,
    borderWidth: Dp = 1.dp,
    blurRadius: Dp = 16.dp,
    specularHighlight: Boolean = true,
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(shape)
            .border(borderWidth, borderColor, shape),
        contentAlignment = contentAlignment
    ) {
        // Blurred backdrop fill layer
        Box(
            modifier = Modifier
                .matchParentSize()
                .then(
                    if (blurRadius > 0.dp) {
                        Modifier.blur(blurRadius, BlurredEdgeTreatment(shape))
                    } else {
                        Modifier
                    }
                )
                .background(backgroundColor, shape)
                .then(
                    if (specularHighlight) {
                        Modifier.drawBehind {
                            drawRect(
                                brush = Brush.verticalGradient(
                                    0.0f to Color.White.copy(alpha = 0.12f),
                                    0.35f to Color.White.copy(alpha = 0.04f),
                                    1.0f to Color.Transparent
                                )
                            )
                        }
                    } else {
                        Modifier
                    }
                )
        )
        // Sharp, unblurred foreground content
        content()
    }
}

/**
 * Reusable GlassCard composable providing the signature Liquid Glass visual effect:
 * - Semi-transparent surfaces with subtle depth and specular reflection
 * - Subtle borders that catch overhead light
 * - Soft inner shadows simulating optical refraction and plate beveling
 * - Optional spring-interactive click feedback with tactile haptics
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    shape: Shape = RoundedCornerShape(cornerRadius),
    backgroundColor: Color = GlassSurface,
    backgroundBrush: Brush? = null,
    borderColor: Color = GlassBorder,
    borderBrush: Brush? = null,
    borderWidth: Dp = 1.dp,
    blurRadius: Dp = 0.dp,
    innerShadowColor: Color = Color.Black.copy(alpha = 0.35f),
    innerShadowBlur: Dp = 16.dp,
    showInnerShadow: Boolean = true,
    specularHighlight: Boolean = true,
    specularAlpha: Float = 0.12f,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val haptic = LocalHapticManager.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val currentStyle = LocalInterfaceStyle.current
    val currentAccent = LocalAccentColor.current

    val effectiveBg = remember(backgroundColor, currentStyle) {
        if (backgroundColor == GlassSurface) {
            when (currentStyle) {
                InterfaceStyle.PURE_LIQUID_GLASS -> Color(0x22FFFFFF)
                InterfaceStyle.MINIMAL -> Color(0xFF1C1C1E)
                InterfaceStyle.MIX_LIQUID -> GlassSurface
            }
        } else backgroundColor
    }

    val effectiveSpecular = when (currentStyle) {
        InterfaceStyle.MINIMAL -> false
        else -> specularHighlight
    }

    val effectiveSpecularAlpha = when (currentStyle) {
        InterfaceStyle.PURE_LIQUID_GLASS -> (specularAlpha * 2.0f).coerceAtMost(0.32f)
        InterfaceStyle.MINIMAL -> 0f
        InterfaceStyle.MIX_LIQUID -> specularAlpha
    }

    val effectiveShowInnerShadow = when (currentStyle) {
        InterfaceStyle.MINIMAL -> false
        else -> showInnerShadow
    }

    val effectiveBorderBrush = borderBrush ?: remember(borderColor, currentStyle, currentAccent) {
        if (borderColor == GlassBorder) {
            when (currentStyle) {
                InterfaceStyle.PURE_LIQUID_GLASS -> {
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.72f),
                            currentAccent.light.copy(alpha = 0.45f),
                            currentAccent.primary.copy(alpha = 0.22f),
                            Color.White.copy(alpha = 0.12f)
                        )
                    )
                }
                InterfaceStyle.MINIMAL -> {
                    SolidColor(Color(0xFF2C2C2E))
                }
                InterfaceStyle.MIX_LIQUID -> {
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.32f),
                            Color.White.copy(alpha = 0.16f),
                            Color.White.copy(alpha = 0.08f)
                        )
                    )
                }
            }
        } else {
            SolidColor(borderColor)
        }
    }

    val effectiveBorderWidth = remember(borderWidth, currentStyle) {
        when (currentStyle) {
            InterfaceStyle.PURE_LIQUID_GLASS -> (borderWidth * 1.2f).coerceAtLeast(1.2.dp)
            InterfaceStyle.MINIMAL -> 0.85.dp
            InterfaceStyle.MIX_LIQUID -> borderWidth
        }
    }

    val scale by animateFloatAsState(
        targetValue = if (onClick != null && isPressed) 0.98f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "glass_card_press_scale"
    )

    val clickModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = {
                haptic.buttonClick()
                onClick()
            }
        )
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .scale(scale)
            .clip(shape)
            .then(
                if (blurRadius > 0.dp) {
                    Modifier.blur(blurRadius, BlurredEdgeTreatment(shape))
                } else {
                    Modifier
                }
            )
            .drawBehind {
                // 1. Semi-transparent surface background
                if (backgroundBrush != null) {
                    drawRect(brush = backgroundBrush)
                } else {
                    drawRect(color = effectiveBg)
                }

                // 2. Specular reflection highlight sheen
                if (effectiveSpecular) {
                    drawRect(
                        brush = Brush.verticalGradient(
                            0.0f to Color.White.copy(alpha = effectiveSpecularAlpha),
                            0.30f to Color.White.copy(alpha = effectiveSpecularAlpha * 0.35f),
                            1.0f to Color.Transparent
                        )
                    )
                }

                // 3. Soft inner shadows for depth and glass beveling
                if (effectiveShowInnerShadow) {
                    val shadowHeight = innerShadowBlur.toPx()
                    if (shadowHeight > 0f) {
                        // Top inner shadow (recessed depth / overhead occlusion)
                        drawRect(
                            brush = Brush.verticalGradient(
                                0.0f to innerShadowColor,
                                0.45f to innerShadowColor.copy(alpha = innerShadowColor.alpha * 0.45f),
                                1.0f to Color.Transparent,
                                startY = 0f,
                                endY = shadowHeight
                            )
                        )
                        // Left subtle inner shadow for dimensional relief
                        drawRect(
                            brush = Brush.horizontalGradient(
                                0.0f to innerShadowColor.copy(alpha = innerShadowColor.alpha * 0.28f),
                                1.0f to Color.Transparent,
                                startX = 0f,
                                endX = shadowHeight * 0.65f
                            )
                        )
                        // Bottom subtle ambient reflection / illumination
                        drawRect(
                            brush = Brush.verticalGradient(
                                0.0f to Color.Transparent,
                                1.0f to Color.White.copy(alpha = 0.04f),
                                startY = (size.height - shadowHeight * 0.5f).coerceAtLeast(0f),
                                endY = size.height
                            )
                        )
                    }
                }
            }
            .border(effectiveBorderWidth, effectiveBorderBrush, shape)
            .then(clickModifier)
    ) {
        Column(
            modifier = Modifier.padding(contentPadding)
        ) {
            content()
        }
    }
}

/**
 * Box-layout variant of [GlassCard] for custom positioning and alignment of children.
 */
@Composable
fun GlassCardBox(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    shape: Shape = RoundedCornerShape(cornerRadius),
    backgroundColor: Color = GlassSurface,
    backgroundBrush: Brush? = null,
    borderColor: Color = GlassBorder,
    borderBrush: Brush? = null,
    borderWidth: Dp = 1.dp,
    blurRadius: Dp = 0.dp,
    innerShadowColor: Color = Color.Black.copy(alpha = 0.35f),
    innerShadowBlur: Dp = 16.dp,
    showInnerShadow: Boolean = true,
    specularHighlight: Boolean = true,
    specularAlpha: Float = 0.12f,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    contentAlignment: Alignment = Alignment.TopStart,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val haptic = LocalHapticManager.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val currentStyle = LocalInterfaceStyle.current
    val currentAccent = LocalAccentColor.current

    val effectiveBg = remember(backgroundColor, currentStyle) {
        if (backgroundColor == GlassSurface) {
            when (currentStyle) {
                InterfaceStyle.PURE_LIQUID_GLASS -> Color(0x22FFFFFF)
                InterfaceStyle.MINIMAL -> Color(0xFF1C1C1E)
                InterfaceStyle.MIX_LIQUID -> GlassSurface
            }
        } else backgroundColor
    }

    val effectiveSpecular = when (currentStyle) {
        InterfaceStyle.MINIMAL -> false
        else -> specularHighlight
    }

    val effectiveSpecularAlpha = when (currentStyle) {
        InterfaceStyle.PURE_LIQUID_GLASS -> (specularAlpha * 2.0f).coerceAtMost(0.32f)
        InterfaceStyle.MINIMAL -> 0f
        InterfaceStyle.MIX_LIQUID -> specularAlpha
    }

    val effectiveShowInnerShadow = when (currentStyle) {
        InterfaceStyle.MINIMAL -> false
        else -> showInnerShadow
    }

    val effectiveBorderBrush = borderBrush ?: remember(borderColor, currentStyle, currentAccent) {
        if (borderColor == GlassBorder) {
            when (currentStyle) {
                InterfaceStyle.PURE_LIQUID_GLASS -> {
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.72f),
                            currentAccent.light.copy(alpha = 0.45f),
                            currentAccent.primary.copy(alpha = 0.22f),
                            Color.White.copy(alpha = 0.12f)
                        )
                    )
                }
                InterfaceStyle.MINIMAL -> {
                    SolidColor(Color(0xFF2C2C2E))
                }
                InterfaceStyle.MIX_LIQUID -> {
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.32f),
                            Color.White.copy(alpha = 0.16f),
                            Color.White.copy(alpha = 0.08f)
                        )
                    )
                }
            }
        } else {
            SolidColor(borderColor)
        }
    }

    val effectiveBorderWidth = remember(borderWidth, currentStyle) {
        when (currentStyle) {
            InterfaceStyle.PURE_LIQUID_GLASS -> (borderWidth * 1.2f).coerceAtLeast(1.2.dp)
            InterfaceStyle.MINIMAL -> 0.85.dp
            InterfaceStyle.MIX_LIQUID -> borderWidth
        }
    }

    val scale by animateFloatAsState(
        targetValue = if (onClick != null && isPressed) 0.98f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "glass_card_box_press_scale"
    )

    val clickModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = {
                haptic.buttonClick()
                onClick()
            }
        )
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .scale(scale)
            .clip(shape)
            .then(
                if (blurRadius > 0.dp) {
                    Modifier.blur(blurRadius, BlurredEdgeTreatment(shape))
                } else {
                    Modifier
                }
            )
            .drawBehind {
                if (backgroundBrush != null) {
                    drawRect(brush = backgroundBrush)
                } else {
                    drawRect(color = effectiveBg)
                }

                if (effectiveSpecular) {
                    drawRect(
                        brush = Brush.verticalGradient(
                            0.0f to Color.White.copy(alpha = effectiveSpecularAlpha),
                            0.30f to Color.White.copy(alpha = effectiveSpecularAlpha * 0.35f),
                            1.0f to Color.Transparent
                        )
                    )
                }

                if (effectiveShowInnerShadow) {
                    val shadowHeight = innerShadowBlur.toPx()
                    if (shadowHeight > 0f) {
                        drawRect(
                            brush = Brush.verticalGradient(
                                0.0f to innerShadowColor,
                                0.45f to innerShadowColor.copy(alpha = innerShadowColor.alpha * 0.45f),
                                1.0f to Color.Transparent,
                                startY = 0f,
                                endY = shadowHeight
                            )
                        )
                        drawRect(
                            brush = Brush.horizontalGradient(
                                0.0f to innerShadowColor.copy(alpha = innerShadowColor.alpha * 0.28f),
                                1.0f to Color.Transparent,
                                startX = 0f,
                                endX = shadowHeight * 0.65f
                            )
                        )
                    }
                }
            }
            .border(effectiveBorderWidth, effectiveBorderBrush, shape)
            .then(clickModifier)
            .padding(contentPadding),
        contentAlignment = contentAlignment
    ) {
        content()
    }
}

/**
 * Modifier extension to apply the signature GlassCard effect with semi-transparent surfaces,
 * subtle borders, and soft inner shadows to any layout.
 */
fun Modifier.glassCard(
    shape: Shape = RoundedCornerShape(24.dp),
    backgroundColor: Color = GlassSurface,
    borderColor: Color = GlassBorder,
    borderWidth: Dp = 1.dp,
    blurRadius: Dp = 0.dp,
    innerShadowColor: Color = Color.Black.copy(alpha = 0.35f),
    innerShadowBlur: Dp = 16.dp,
    showInnerShadow: Boolean = true,
    specularHighlight: Boolean = true,
    specularAlpha: Float = 0.12f
): Modifier = this
    .clip(shape)
    .then(
        if (blurRadius > 0.dp) {
            Modifier.blur(blurRadius, BlurredEdgeTreatment(shape))
        } else {
            Modifier
        }
    )
    .drawBehind {
        drawRect(color = backgroundColor)
        if (specularHighlight) {
            drawRect(
                brush = Brush.verticalGradient(
                    0.0f to Color.White.copy(alpha = specularAlpha),
                    0.30f to Color.White.copy(alpha = specularAlpha * 0.35f),
                    1.0f to Color.Transparent
                )
            )
        }
        if (showInnerShadow) {
            val shadowHeight = innerShadowBlur.toPx()
            if (shadowHeight > 0f) {
                drawRect(
                    brush = Brush.verticalGradient(
                        0.0f to innerShadowColor,
                        0.45f to innerShadowColor.copy(alpha = innerShadowColor.alpha * 0.45f),
                        1.0f to Color.Transparent,
                        startY = 0f,
                        endY = shadowHeight
                    )
                )
                drawRect(
                    brush = Brush.horizontalGradient(
                        0.0f to innerShadowColor.copy(alpha = innerShadowColor.alpha * 0.28f),
                        1.0f to Color.Transparent,
                        startX = 0f,
                        endX = shadowHeight * 0.65f
                    )
                )
            }
        }
    }
    .border(
        borderWidth,
        if (borderColor == GlassBorder) {
            Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = 0.32f),
                    Color.White.copy(alpha = 0.16f),
                    Color.White.copy(alpha = 0.08f)
                )
            )
        } else {
            SolidColor(borderColor)
        },
        shape
    )

/**
 * High-end Liquid Glass Card delegating to [GlassCard] for consistency and soft inner shadows.
 */
@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 28.dp,
    shape: Shape = RoundedCornerShape(cornerRadius),
    backgroundColor: Color = GlassSurface,
    borderColor: Color = GlassBorder,
    borderWidth: Dp = 1.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    GlassCard(
        modifier = modifier,
        cornerRadius = cornerRadius,
        shape = shape,
        backgroundColor = backgroundColor,
        borderColor = borderColor,
        borderWidth = borderWidth,
        content = content
    )
}

/**
 * Tactile spring-interactive Liquid Glass Button.
 */
@Composable
fun LiquidGlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    backgroundColor: Color = GlassSurfaceElevated,
    borderColor: Color = GlassBorderBright,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val haptic = LocalHapticManager.current

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "glass_button_scale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .clip(shape)
            .background(backgroundColor)
            .border(1.dp, borderColor, shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    haptic.buttonClick()
                    onClick()
                }
            )
            .padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/**
 * Slide to Dismiss Liquid Glass Slider.
 * Provides physical drag feedback and springs back if released before the threshold.
 */
@Composable
fun LiquidDismissSlider(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticManager.current
    val scope = rememberCoroutineScope()
    val dragOffset = remember { Animatable(0f) }
    var trackWidthPx by remember { mutableFloatStateOf(0f) }
    val handleSize = 64.dp
    val density = LocalDensity.current
    val handleSizePx = with(density) { handleSize.toPx() }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(76.dp)
            .clip(CircleShape)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.08f),
                        Color.White.copy(alpha = 0.14f),
                        Color.White.copy(alpha = 0.08f)
                    )
                )
            )
            .border(1.dp, GlassBorderBright, CircleShape)
            .padding(6.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        trackWidthPx = with(density) { maxWidth.toPx() }
        val maxDrag = (trackWidthPx - handleSizePx - with(density) { 12.dp.toPx() }).coerceAtLeast(0f)

        // Shimmering instruction text
        val progress = if (maxDrag > 0f) (dragOffset.value / maxDrag).coerceIn(0f, 1f) else 0f
        val textAlpha = (1f - progress * 1.5f).coerceIn(0f, 1f)

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Slide to dismiss  →",
                color = TextSecondary.copy(alpha = textAlpha),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.8.sp
            )
        }

        // Draggable Glass Handle
        Box(
            modifier = Modifier
                .offset { IntOffset(dragOffset.value.roundToInt(), 0) }
                .size(handleSize)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.95f),
                            Color.White.copy(alpha = 0.75f)
                        )
                    )
                )
                .border(1.5.dp, Color.White, CircleShape)
                .pointerInput(maxDrag) {
                    detectHorizontalDragGestures(
                        onDragStart = {
                            haptic.sliderDragStart()
                        },
                        onDragEnd = {
                            if (dragOffset.value >= maxDrag * 0.75f) {
                                // Reached threshold - snap to end, affirmative confirm haptic, then dismiss
                                scope.launch {
                                    dragOffset.animateTo(
                                        maxDrag,
                                        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessHigh)
                                    )
                                    haptic.confirm()
                                    onDismiss()
                                }
                            } else {
                                // Snap back with smooth spring
                                haptic.sliderDragEnd()
                                scope.launch {
                                    dragOffset.animateTo(
                                        0f,
                                        spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium)
                                    )
                                }
                            }
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            scope.launch {
                                val prev = dragOffset.value
                                val next = (prev + dragAmount).coerceIn(0f, maxDrag)
                                dragOffset.snapTo(next)

                                // Subtle tick for incremental drag movement every ~24dp
                                val tickStep = with(density) { 24.dp.toPx() }
                                if (tickStep > 0 && (next / tickStep).toInt() != (prev / tickStep).toInt()) {
                                    haptic.sliderTick()
                                }

                                // Milestone snap when crossing threshold
                                val threshold = maxDrag * 0.75f
                                if (next >= threshold && prev < threshold) {
                                    haptic.sliderSnap()
                                }
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Slide Arrow",
                tint = Color.Black.copy(alpha = 0.85f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * Floating Liquid Glass Navigation Bar with fluid liquid morphing capsule.
 */
enum class ClockNavTab(val title: String, val icon: ImageVector) {
    WORLD("World Clock", Icons.Default.Public),
    ALARMS("Alarms", Icons.Default.Alarm),
    STOPWATCH("Stopwatch", Icons.Default.Timer),
    TIMERS("Timers", Icons.Default.HourglassEmpty)
}

@Composable
fun LiquidFloatingNavBar(
    selectedTab: ClockNavTab,
    onTabSelected: (ClockNavTab) -> Unit,
    modifier: Modifier = Modifier,
    pagerOffsetFractionProvider: (() -> Float)? = null
) {
    val haptic = LocalHapticManager.current
    val tabs = remember { ClockNavTab.values() }
    val tabCount = tabs.size
    val selectedIndex = tabs.indexOf(selectedTab)

    // Dynamic Liquid Glass accent palette
    val currentAccent = LocalAccentColor.current
    val lightBluePrimary = currentAccent.primary
    val lightBlueLight = currentAccent.light
    val lightBlueDark = currentAccent.dark

    val coroutineScope = rememberCoroutineScope()
    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(selectedIndex.toFloat()) }
    var lastHapticIndex by remember { mutableIntStateOf(selectedIndex) }
    var pressedTabIndex by remember { mutableStateOf<Int?>(null) }

    // Liquid stretching capsule bounds (start and end edge animations)
    val animStart = remember { Animatable(selectedIndex.toFloat()) }
    val animEnd = remember { Animatable((selectedIndex + 1).toFloat()) }

    // Synchronize animators when not dragging
    if (pagerOffsetFractionProvider != null) {
        val pagerPosition = pagerOffsetFractionProvider()
        LaunchedEffect(pagerPosition, isDragging) {
            if (!isDragging) {
                val clamped = pagerPosition.coerceIn(0f, (tabCount - 1).toFloat())
                val targetStart = clamped
                val targetEnd = clamped + 1f
                val movingRight = targetStart > animStart.value

                launch {
                    if (movingRight) {
                        animEnd.animateTo(
                            targetValue = targetEnd,
                            animationSpec = spring(
                                dampingRatio = 0.70f,
                                stiffness = 360f
                            )
                        )
                    } else {
                        animStart.animateTo(
                            targetValue = targetStart,
                            animationSpec = spring(
                                dampingRatio = 0.70f,
                                stiffness = 360f
                            )
                        )
                    }
                }

                launch {
                    if (movingRight) {
                        animStart.animateTo(
                            targetValue = targetStart,
                            animationSpec = spring(
                                dampingRatio = 0.78f,
                                stiffness = 260f
                            )
                        )
                    } else {
                        animEnd.animateTo(
                            targetValue = targetEnd,
                            animationSpec = spring(
                                dampingRatio = 0.78f,
                                stiffness = 260f
                            )
                        )
                    }
                }
            }
        }
    } else {
        LaunchedEffect(selectedIndex, isDragging) {
            if (!isDragging) {
                val targetStart = selectedIndex.toFloat()
                val targetEnd = (selectedIndex + 1).toFloat()
                val movingRight = targetStart > animStart.value

                // Fluid viscous liquid motion:
                // Leading edge surges forward with high-responsiveness bouncy spring,
                // while trailing edge follows smoothly, stretching the liquid droplet in between!
                launch {
                    if (movingRight) {
                        animEnd.animateTo(
                            targetValue = targetEnd,
                            animationSpec = spring(
                                dampingRatio = 0.68f,
                                stiffness = 380f
                            )
                        )
                    } else {
                        animStart.animateTo(
                            targetValue = targetStart,
                            animationSpec = spring(
                                dampingRatio = 0.68f,
                                stiffness = 380f
                            )
                        )
                    }
                }

                launch {
                    if (movingRight) {
                        animStart.animateTo(
                            targetValue = targetStart,
                            animationSpec = spring(
                                dampingRatio = 0.78f,
                                stiffness = 240f
                            )
                        )
                    } else {
                        animEnd.animateTo(
                            targetValue = targetEnd,
                            animationSpec = spring(
                                dampingRatio = 0.78f,
                                stiffness = 240f
                            )
                        )
                    }
                }
            }
        }
    }

    val currentStyle = LocalInterfaceStyle.current

    val dockBrush = remember(currentStyle, lightBluePrimary) {
        when (currentStyle) {
            InterfaceStyle.PURE_LIQUID_GLASS -> Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = 0.18f),
                    Color.White.copy(alpha = 0.08f),
                    Color(0xFF0F1118).copy(alpha = 0.88f)
                )
            )
            InterfaceStyle.MINIMAL -> Brush.verticalGradient(
                listOf(
                    Color(0xFF10121A),
                    Color(0xFF0A0B10)
                )
            )
            InterfaceStyle.MIX_LIQUID -> Brush.verticalGradient(
                listOf(
                    Color(0xFF222226).copy(alpha = 0.88f),
                    Color(0xFF141416).copy(alpha = 0.94f)
                )
            )
        }
    }

    val dockBorderBrush = remember(currentStyle, lightBlueLight) {
        when (currentStyle) {
            InterfaceStyle.PURE_LIQUID_GLASS -> Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = 0.65f),
                    lightBlueLight.copy(alpha = 0.45f),
                    Color.White.copy(alpha = 0.18f)
                )
            )
            InterfaceStyle.MINIMAL -> SolidColor(Color.White.copy(alpha = 0.12f))
            InterfaceStyle.MIX_LIQUID -> Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = 0.22f),
                    Color.White.copy(alpha = 0.08f)
                )
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp)
                .clip(CircleShape)
                .background(dockBrush)
                .border(
                    width = if (currentStyle == InterfaceStyle.MINIMAL) 0.85.dp else 1.dp,
                    brush = dockBorderBrush,
                    shape = CircleShape
                )
                .padding(horizontal = 6.dp, vertical = 6.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            val totalWidth = maxWidth
            val tabWidth = totalWidth / tabCount
            val density = LocalDensity.current
            val tabWidthPx = with(density) { tabWidth.toPx() }

            val currentStart = animStart.value
            val currentEnd = animEnd.value
            val span = (currentEnd - currentStart).coerceAtLeast(0.7f)
            val stretch = (span - 1f).coerceAtLeast(0f)

            // Liquid volume conservation: slight vertical compression while stretching horizontally
            val squishY = (1f - stretch * 0.12f).coerceIn(0.85f, 1f)

            val offsetX = tabWidth * currentStart
            val pillWidth = tabWidth * span

            // Interactive horizontal sliding gesture & instant tap handler on navigation bar
            val navGestureModifier = Modifier.pointerInput(tabWidthPx, tabCount) {
                if (tabWidthPx <= 0f) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val downX = down.position.x
                    var currentX = downX
                    var hasDragged = false
                    val touchSlop = viewConfiguration.touchSlop

                    val touchedTab = (downX / tabWidthPx).toInt().coerceIn(0, tabCount - 1)
                    pressedTabIndex = touchedTab

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break

                        if (!change.pressed) {
                            pressedTabIndex = null
                            if (hasDragged) {
                                isDragging = false
                                val targetIndex = dragFraction.roundToInt().coerceIn(0, tabCount - 1)
                                haptic.sliderSnap()
                                onTabSelected(tabs[targetIndex])
                                coroutineScope.launch {
                                    animStart.animateTo(
                                        targetValue = targetIndex.toFloat(),
                                        animationSpec = spring(dampingRatio = 0.72f, stiffness = 380f)
                                    )
                                }
                                coroutineScope.launch {
                                    animEnd.animateTo(
                                        targetValue = (targetIndex + 1).toFloat(),
                                        animationSpec = spring(dampingRatio = 0.68f, stiffness = 400f)
                                    )
                                }
                            } else {
                                val tappedIndex = (downX / tabWidthPx).toInt().coerceIn(0, tabCount - 1)
                                haptic.buttonClick()
                                onTabSelected(tabs[tappedIndex])
                            }
                            change.consume()
                            break
                        } else {
                            val newX = change.position.x
                            val totalDeltaX = newX - downX

                            if (!hasDragged && abs(totalDeltaX) > touchSlop) {
                                hasDragged = true
                                isDragging = true
                                haptic.sliderDragStart()
                                val initialCenter = (newX / tabWidthPx).coerceIn(0.5f, tabCount - 0.5f)
                                val frac = initialCenter - 0.5f
                                dragFraction = frac
                                lastHapticIndex = frac.roundToInt().coerceIn(0, tabCount - 1)
                                coroutineScope.launch {
                                    animStart.snapTo(frac)
                                    animEnd.snapTo(frac + 1f)
                                }
                            }

                            if (hasDragged) {
                                val deltaX = newX - currentX
                                currentX = newX
                                change.consume()

                                val currentCenter = (newX / tabWidthPx).coerceIn(0.5f, tabCount - 0.5f)
                                val frac = currentCenter - 0.5f
                                dragFraction = frac
                                pressedTabIndex = frac.roundToInt().coerceIn(0, tabCount - 1)

                                val currentHovered = frac.roundToInt().coerceIn(0, tabCount - 1)
                                if (currentHovered != lastHapticIndex) {
                                    lastHapticIndex = currentHovered
                                    haptic.buttonClick()
                                    haptic.sliderTick()
                                }

                                val deltaFrac = deltaX / tabWidthPx
                                coroutineScope.launch {
                                    if (deltaFrac > 0) {
                                        val stretchLead = (deltaFrac * 1.8f).coerceAtMost(0.45f)
                                        animEnd.snapTo((frac + 1f + stretchLead).coerceAtMost(tabCount.toFloat()))
                                        animStart.snapTo(frac)
                                    } else if (deltaFrac < 0) {
                                        val stretchTrail = (deltaFrac * 1.8f).coerceAtLeast(-0.45f)
                                        animStart.snapTo((frac + stretchTrail).coerceAtLeast(0f))
                                        animEnd.snapTo(frac + 1f)
                                    } else {
                                        animStart.snapTo(frac)
                                        animEnd.snapTo(frac + 1f)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Animated morphing liquid light blue glass capsule
            val capsuleBackgroundBrush = remember(currentStyle, lightBluePrimary, lightBlueDark) {
                when (currentStyle) {
                    InterfaceStyle.PURE_LIQUID_GLASS -> Brush.verticalGradient(
                        listOf(
                            lightBluePrimary.copy(alpha = 0.38f),
                            lightBlueDark.copy(alpha = 0.24f),
                            Color.White.copy(alpha = 0.14f)
                        )
                    )
                    InterfaceStyle.MINIMAL -> SolidColor(lightBluePrimary.copy(alpha = 0.16f))
                    InterfaceStyle.MIX_LIQUID -> Brush.verticalGradient(
                        listOf(
                            lightBluePrimary.copy(alpha = 0.28f),
                            lightBlueDark.copy(alpha = 0.18f),
                            Color.Black.copy(alpha = 0.22f)
                        )
                    )
                }
            }

            val capsuleBorderBrush = remember(currentStyle, lightBlueLight, lightBluePrimary) {
                when (currentStyle) {
                    InterfaceStyle.PURE_LIQUID_GLASS -> Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.85f),
                            lightBlueLight.copy(alpha = 0.65f),
                            lightBluePrimary.copy(alpha = 0.40f)
                        )
                    )
                    InterfaceStyle.MINIMAL -> SolidColor(lightBluePrimary.copy(alpha = 0.65f))
                    InterfaceStyle.MIX_LIQUID -> Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.65f),
                            lightBlueLight.copy(alpha = 0.45f),
                            lightBluePrimary.copy(alpha = 0.25f)
                        )
                    )
                }
            }

            Box(
                modifier = Modifier
                    .offset(x = offsetX)
                    .width(pillWidth)
                    .height(58.dp)
                    .graphicsLayer {
                        scaleY = squishY
                    }
                    .clip(CircleShape)
                    .background(capsuleBackgroundBrush)
                    .drawBehind {
                        if (currentStyle == InterfaceStyle.PURE_LIQUID_GLASS) {
                            // Enhanced luminous aura glow
                            drawRoundRect(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        lightBluePrimary.copy(alpha = 0.48f),
                                        lightBluePrimary.copy(alpha = 0.15f),
                                        Color.Transparent
                                    ),
                                    center = center,
                                    radius = size.width.coerceAtLeast(size.height) * 0.9f
                                ),
                                cornerRadius = CornerRadius(size.height / 2f, size.height / 2f)
                            )
                            // Top radiant specular glass reflection sheen
                            drawRoundRect(
                                brush = Brush.verticalGradient(
                                    0.0f to Color.White.copy(alpha = 0.60f),
                                    0.40f to lightBlueLight.copy(alpha = 0.30f),
                                    1.0f to Color.Transparent
                                ),
                                cornerRadius = CornerRadius(size.height / 2f, size.height / 2f)
                            )
                        } else if (currentStyle == InterfaceStyle.MIX_LIQUID) {
                            // Soft liquid aura glow
                            drawRoundRect(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        lightBluePrimary.copy(alpha = 0.35f),
                                        lightBluePrimary.copy(alpha = 0.08f),
                                        Color.Transparent
                                    ),
                                    center = center,
                                    radius = size.width.coerceAtLeast(size.height) * 0.7f
                                ),
                                cornerRadius = CornerRadius(size.height / 2f, size.height / 2f)
                            )
                            // Top specular liquid reflection sheen
                            drawRoundRect(
                                brush = Brush.verticalGradient(
                                    0.0f to Color.White.copy(alpha = 0.40f),
                                    0.35f to lightBlueLight.copy(alpha = 0.20f),
                                    1.0f to Color.Transparent
                                ),
                                cornerRadius = CornerRadius(size.height / 2f, size.height / 2f)
                            )
                        }
                    }
                    .border(
                        width = if (currentStyle == InterfaceStyle.MINIMAL) 1.dp else 1.2.dp,
                        brush = capsuleBorderBrush,
                        shape = CircleShape
                    )
            )

            // Navigation items row with integrated drag & tap gesture modifier
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .then(navGestureModifier),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val highlightedIndex = if (isDragging) {
                    dragFraction.roundToInt().coerceIn(0, tabCount - 1)
                } else {
                    ((animStart.value + animEnd.value) / 2f).roundToInt().coerceIn(0, tabCount - 1)
                }

                tabs.forEach { tab ->
                    val tabIdx = tabs.indexOf(tab)
                    val isSelected = (tab == selectedTab) || (tabIdx == highlightedIndex)
                    val isPressed = (pressedTabIndex == tabIdx)

                    // Tactile press bounce
                    val buttonPressScale by animateFloatAsState(
                        targetValue = if (isPressed) 0.90f else 1.0f,
                        animationSpec = spring(
                            dampingRatio = 0.65f,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "tab_press_scale"
                    )

                    // Active icon lively bounce & pop
                    val iconScale by animateFloatAsState(
                        targetValue = if (isSelected) 1.15f else 1.0f,
                        animationSpec = spring(
                            dampingRatio = 0.52f,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        label = "icon_pop_scale"
                    )

                    val iconOffsetY by animateFloatAsState(
                        targetValue = if (isSelected) -2f else 0f,
                        animationSpec = spring(
                            dampingRatio = 0.55f,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        label = "icon_offset_y"
                    )

                    val iconColor by animateColorAsState(
                        targetValue = if (isSelected) lightBluePrimary else Color.White.copy(alpha = 0.85f),
                        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
                        label = "icon_color"
                    )

                    val textColor by animateColorAsState(
                        targetValue = if (isSelected) lightBluePrimary else Color(0xFF8E8E93),
                        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
                        label = "text_color"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(58.dp)
                            .graphicsLayer {
                                scaleX = buttonPressScale
                                scaleY = buttonPressScale
                            }
                            .clip(CircleShape)
                            .testTag("nav_tab_${tab.name.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title,
                                tint = iconColor,
                                modifier = Modifier
                                    .size(21.dp)
                                    .offset(y = iconOffsetY.dp)
                                    .graphicsLayer {
                                        scaleX = iconScale
                                        scaleY = iconScale
                                    }
                            )
                            Text(
                                text = tab.title,
                                color = textColor,
                                fontSize = 10.5.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                letterSpacing = 0.15.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
