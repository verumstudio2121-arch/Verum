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
    primaryContainer = DarkGlassSurfaceElevated,
    onPrimaryContainer = Color.White,
    secondary = accentColor,
    onSecondary = DeepBlack,
    background = DeepBlack,
    onBackground = DarkTextPrimary,
    surface = ObsidianBlack,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkGlassSurface,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkGlassBorder
)

private fun createHiTechLightColorScheme(accentColor: Color) = lightColorScheme(
    primary = LightInk,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE6ECF5),
    onPrimaryContainer = LightInk,
    secondary = accentColor,
    onSecondary = Color.White,
    background = LightCanvas,
    onBackground = LightInk,
    surface = LightSurface,
    onSurface = LightInk,
    surfaceVariant = Color(0xFFE8EEF7),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0x1F0F172A)
)

@Composable
fun HiTechClockTheme(
    darkTheme: Boolean = true,
    accentTheme: AccentTheme = AccentColorTheme.LIGHT_BLUE,
    interfaceStyle: InterfaceStyle = InterfaceStyle.MIX_LIQUID,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        val base = createHiTechDarkColorScheme(accentTheme.primary)
        when (interfaceStyle) {
            InterfaceStyle.MINIMAL -> base.copy(
                background = DeepBlack,
                surface = Color(0xFF0F1017),
                outline = Color(0x22FFFFFF)
            )
            InterfaceStyle.PURE_LIQUID_GLASS -> base.copy(
                background = DeepBlack,
                surface = Color(0x1AFFFFFF),
                outline = Color(0x40FFFFFF)
            )
            InterfaceStyle.MIX_LIQUID -> base
        }
    } else {
        val base = createHiTechLightColorScheme(accentTheme.primary)
        when (interfaceStyle) {
            InterfaceStyle.MINIMAL -> base.copy(
                background = Color(0xFFF5F6F8),
                surface = Color.White,
                outline = Color(0xFFE2E8F0)
            )
            InterfaceStyle.PURE_LIQUID_GLASS -> base.copy(
                background = Color(0xFFEEF3FA),
                surface = Color(0xD9FFFFFF),
                outline = Color(0x280F172A)
            )
            InterfaceStyle.MIX_LIQUID -> base
        }
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                @Suppress("DEPRECATION")
                window.statusBarColor = Color.Transparent.toArgb()
                @Suppress("DEPRECATION")
                window.navigationBarColor = Color.Transparent.toArgb()
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !darkTheme
                controller.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(
        LocalIsDarkTheme provides darkTheme,
        LocalAccentColor provides accentTheme,
        LocalInterfaceStyle provides interfaceStyle
    ) {
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

