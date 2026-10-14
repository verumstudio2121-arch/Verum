package com.example.ui.background

import android.graphics.BitmapFactory
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.example.ui.theme.AccentTheme
import com.example.ui.theme.DeepBlack
import com.example.ui.theme.InterfaceStyle
import java.io.File
import kotlin.random.Random

/**
 * Predefined background options for HiTech Clock.
 * Pure Black is the default background.
 */
enum class BackgroundPreset(
    val id: String,
    val title: String,
    val subtitle: String
) {
    BLACK("Black", "Pure Black", "Default OLED black"),
    COSMOS("Cosmos", "Deep Cosmos", "Starfield nebula & deep space"),
    CYBER_GRID("Cyber Grid", "Cyber Grid", "Neon synth perspective grid"),
    AURORA_WAVE("Aurora Wave", "Liquid Aurora", "Flowing glass waves & refractions"),
    MIDNIGHT_DEEP("Midnight Deep", "Midnight Abyss", "Deep oceanic vignette"),
    MINIMAL_SLATE("Minimal Slate", "Minimal Slate", "Architectural graphite texture"),
    CUSTOM_PHOTO("Custom Photo", "Custom Photo", "Photo from your gallery");

    companion object {
        fun fromId(id: String): BackgroundPreset {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: BLACK
        }
    }
}

/**
 * Stable star particle for the Cosmos procedural background.
 */
private data class StarParticle(
    val xRatio: Float,
    val yRatio: Float,
    val radius: Float,
    val alpha: Float
)

/**
 * Global background layer rendered behind all application screens.
 */
@Composable
fun AppBackground(
    backgroundType: String,
    customPhotoPath: String?,
    backgroundDim: Float,
    interfaceStyle: InterfaceStyle,
    accentTheme: AccentTheme,
    modifier: Modifier = Modifier
) {
    val preset = remember(backgroundType) { BackgroundPreset.fromId(backgroundType) }

    Box(modifier = modifier.fillMaxSize()) {
        Crossfade(
            targetState = preset,
            animationSpec = tween(durationMillis = 350),
            label = "BackgroundCrossfade"
        ) { targetPreset ->
            when (targetPreset) {
                BackgroundPreset.BLACK -> {
                    // Default Pure Black
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black)
                    )
                }

                BackgroundPreset.COSMOS -> {
                    CosmosBackground(accentColor = accentTheme.primary)
                }

                BackgroundPreset.CYBER_GRID -> {
                    CyberGridBackground(accentColor = accentTheme.primary)
                }

                BackgroundPreset.AURORA_WAVE -> {
                    AuroraWaveBackground(
                        primaryColor = accentTheme.primary,
                        lightColor = accentTheme.light
                    )
                }

                BackgroundPreset.MIDNIGHT_DEEP -> {
                    MidnightDeepBackground(accentColor = accentTheme.primary)
                }

                BackgroundPreset.MINIMAL_SLATE -> {
                    MinimalSlateBackground()
                }

                BackgroundPreset.CUSTOM_PHOTO -> {
                    CustomPhotoBackground(customPhotoPath = customPhotoPath)
                }
            }
        }

        // Configurable Dark Dimming Scrim (applied over non-black backgrounds or custom photo)
        if (preset != BackgroundPreset.BLACK) {
            val scrimAlpha = backgroundDim.coerceIn(0.15f, 0.85f)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = scrimAlpha))
            )
        }

        // Liquid Glass Theme Signature Ambient Refractions
        if (interfaceStyle == InterfaceStyle.PURE_LIQUID_GLASS) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            accentTheme.primary.copy(alpha = 0.12f),
                            accentTheme.dark.copy(alpha = 0.04f),
                            Color.Transparent
                        ),
                        center = Offset(canvasWidth * 0.5f, canvasHeight * 0.22f),
                        radius = canvasWidth * 0.85f
                    )
                )

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            accentTheme.light.copy(alpha = 0.07f),
                            Color.Transparent
                        ),
                        center = Offset(canvasWidth * 0.85f, canvasHeight * 0.80f),
                        radius = canvasWidth * 0.65f
                    )
                )
            }
        }
    }
}

/**
 * Procedural Deep Cosmos starfield and nebula clouds.
 */
@Composable
private fun CosmosBackground(
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val stars = remember {
        val rand = Random(42)
        List(110) {
            StarParticle(
                xRatio = rand.nextFloat(),
                yRatio = rand.nextFloat(),
                radius = rand.nextFloat() * 1.8f + 0.6f,
                alpha = rand.nextFloat() * 0.65f + 0.25f
            )
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF030408))
    ) {
        val w = size.width
        val h = size.height

        // Deep galactic nebula glow 1
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    accentColor.copy(alpha = 0.20f),
                    Color(0xFF3B1E63).copy(alpha = 0.14f),
                    Color.Transparent
                ),
                center = Offset(w * 0.65f, h * 0.35f),
                radius = w * 0.95f
            )
        )

        // Galactic nebula glow 2
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF1E3A8A).copy(alpha = 0.22f),
                    Color(0xFF0F172A).copy(alpha = 0.08f),
                    Color.Transparent
                ),
                center = Offset(w * 0.25f, h * 0.72f),
                radius = w * 0.85f
            )
        )

        // Stars
        stars.forEach { star ->
            val cx = star.xRatio * w
            val cy = star.yRatio * h
            drawCircle(
                color = Color.White.copy(alpha = star.alpha),
                radius = star.radius,
                center = Offset(cx, cy)
            )
        }
    }
}

/**
 * Cyberpunk perspective neon wireframe grid with horizon ambient glow.
 */
@Composable
private fun CyberGridBackground(
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF04060C))
    ) {
        val w = size.width
        val h = size.height
        val horizonY = h * 0.52f

        // Horizon ambient glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    accentColor.copy(alpha = 0.26f),
                    accentColor.copy(alpha = 0.08f),
                    Color.Transparent
                ),
                center = Offset(w * 0.5f, horizonY),
                radius = w * 0.8f
            )
        )

        // Horizontal perspective lines below horizon
        val numHorizLines = 14
        for (i in 1..numHorizLines) {
            val t = (i.toFloat() / numHorizLines)
            // Exponential distribution for 3D depth perspective
            val y = horizonY + (h - horizonY) * (t * t)
            val lineAlpha = (0.05f + 0.25f * t).coerceIn(0.05f, 0.30f)
            drawLine(
                color = accentColor.copy(alpha = lineAlpha),
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = 1f + 1.2f * t
            )
        }

        // Perspective rays radiating from vanishing point
        val vp = Offset(w * 0.5f, horizonY)
        val numRays = 12
        for (i in 0..numRays) {
            val bottomX = (w / numRays) * i
            drawLine(
                color = accentColor.copy(alpha = 0.16f),
                start = vp,
                end = Offset(bottomX, h),
                strokeWidth = 1.2f
            )
        }
    }
}

/**
 * Smooth fluid Liquid Aurora waves with organic gradients.
 */
@Composable
private fun AuroraWaveBackground(
    primaryColor: Color,
    lightColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF04060E))
    ) {
        val w = size.width
        val h = size.height

        // Upper ambient wave
        val wavePath1 = Path().apply {
            moveTo(0f, h * 0.15f)
            cubicTo(
                w * 0.35f, h * 0.05f,
                w * 0.65f, h * 0.28f,
                w, h * 0.18f
            )
            lineTo(w, 0f)
            lineTo(0f, 0f)
            close()
        }

        drawPath(
            path = wavePath1,
            brush = Brush.verticalGradient(
                colors = listOf(
                    primaryColor.copy(alpha = 0.22f),
                    Color.Transparent
                ),
                startY = 0f,
                endY = h * 0.35f
            )
        )

        // Middle fluid wave
        val wavePath2 = Path().apply {
            moveTo(0f, h * 0.55f)
            cubicTo(
                w * 0.25f, h * 0.42f,
                w * 0.70f, h * 0.68f,
                w, h * 0.50f
            )
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }

        drawPath(
            path = wavePath2,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    lightColor.copy(alpha = 0.12f),
                    primaryColor.copy(alpha = 0.24f)
                ),
                startY = h * 0.45f,
                endY = h
            )
        )
    }
}

/**
 * Deep oceanic luxury vignette with rich midnight gradients.
 */
@Composable
private fun MidnightDeepBackground(
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF020409))
    ) {
        val w = size.width
        val h = size.height

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF020409),
                    Color(0xFF0B1426),
                    Color(0xFF050811)
                )
            )
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    accentColor.copy(alpha = 0.15f),
                    Color.Transparent
                ),
                center = Offset(w * 0.5f, h * 0.5f),
                radius = w * 0.75f
            )
        )
    }
}

/**
 * Architectural Minimal Slate dark brushed texture.
 */
@Composable
private fun MinimalSlateBackground(
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0B0D11))
    ) {
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF14171E),
                    Color(0xFF0D0F14),
                    Color(0xFF07080B)
                ),
                start = Offset(0f, 0f),
                end = Offset(size.width, size.height)
            )
        )
    }
}

/**
 * User's custom photo background from local storage.
 * Fallbacks cleanly to Black if file does not exist or cannot be decoded.
 */
@Composable
private fun CustomPhotoBackground(
    customPhotoPath: String?,
    modifier: Modifier = Modifier
) {
    val file = remember(customPhotoPath) {
        if (!customPhotoPath.isNullOrEmpty()) File(customPhotoPath) else null
    }

    if (file != null && file.exists() && file.canRead()) {
        val bitmap = remember(file.absolutePath, file.lastModified()) {
            try {
                BitmapFactory.decodeFile(file.absolutePath)
            } catch (_: Exception) {
                null
            }
        }

        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Custom wallpaper",
                contentScale = ContentScale.Crop,
                modifier = modifier.fillMaxSize()
            )
        } else {
            // Coil fallback
            AsyncImage(
                model = file,
                contentDescription = "Custom wallpaper",
                contentScale = ContentScale.Crop,
                modifier = modifier.fillMaxSize()
            )
        }
    } else {
        // Fallback to Pitch Black
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black)
        )
    }
}
