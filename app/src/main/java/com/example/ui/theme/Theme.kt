package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private fun createHiTechDarkColorScheme(accentColor: Color) = darkColorScheme(
    primary = Color.White,
    onPrimary = DeepBlack,
    primaryContainer = GlassSurfaceElevated,
    onPrimaryContainer = Color.White,
    secondary = accentColor,
    onSecondary = DeepBlack,
    background = DeepBlack,
    onBackground = TextPrimary,
    surface = ObsidianBlack,
    onSurface = TextPrimary,
    surfaceVariant = GlassSurface,
    onSurfaceVariant = TextSecondary,
    outline = GlassBorder
)

private fun createHiTechLightColorScheme(accentColor: Color) = lightColorScheme(
    primary = Color.Black,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE5E7EB),
    onPrimaryContainer = Color.Black,
    secondary = accentColor,
    onSecondary = Color.White,
    background = Color(0xFF0F1015),
    onBackground = Color.White,
    surface = Color(0xFF161820),
    onSurface = Color.White,
    surfaceVariant = Color(0x22FFFFFF),
    onSurfaceVariant = Color(0xCCFFFFFF),
    outline = Color(0x33FFFFFF)
)

@Composable
fun HiTechClockTheme(
    darkTheme: Boolean = true,
    accentTheme: AccentColorTheme = AccentColorTheme.LIGHT_BLUE,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        createHiTechDarkColorScheme(accentTheme.primary)
    } else {
        createHiTechLightColorScheme(accentTheme.primary)
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = DeepBlack.toArgb()
                window.navigationBarColor = DeepBlack.toArgb()
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !darkTheme
                controller.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(LocalAccentColor provides accentTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

// Keep backwards-compat alias
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    HiTechClockTheme(darkTheme = darkTheme, content = content)
}

