package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val DeepBlack = Color(0xFF000000)
val ObsidianBlack = Color(0xFF08080C)
val CharcoalGlass = Color(0xFF121218)

// Liquid Glass surfaces
val GlassSurface = Color(0x17FFFFFF)
val GlassSurfaceElevated = Color(0x22FFFFFF)
val GlassSurfaceSubtle = Color(0x0EFFFFFF)
val GlassSelectedCapsule = Color(0x2EFFFFFF)

// Borders & Reflections
val GlassBorder = Color(0x24FFFFFF)
val GlassBorderBright = Color(0x4DFFFFFF)
val GlassHighlight = Color(0x2BFFFFFF)

// Accents
val AccentCyan = Color(0xFF38BDF8)
val AccentOrange = Color(0xFFFF9F0A)
val AccentGreen = Color(0xFF34C759)
val AccentRed = Color(0xFFFF453A)
val AccentViolet = Color(0xFFA78BFA)

/**
 * Liquid Glass dynamic accent color options.
 */
enum class AccentColorTheme(
    val id: String,
    val displayName: String,
    val primary: Color,
    val light: Color,
    val dark: Color
) {
    LIGHT_BLUE(
        id = "Light Blue",
        displayName = "Light Blue",
        primary = Color(0xFF38BDF8),
        light = Color(0xFF80D6FF),
        dark = Color(0xFF0284C7)
    ),
    LAVENDER(
        id = "Lavender",
        displayName = "Lavender",
        primary = Color(0xFFA78BFA),
        light = Color(0xFFC4B5FD),
        dark = Color(0xFF7C3AED)
    ),
    MINT(
        id = "Mint",
        displayName = "Mint",
        primary = Color(0xFF2DD4BF),
        light = Color(0xFF5EEAD4),
        dark = Color(0xFF0F766E)
    ),
    CORAL(
        id = "Coral",
        displayName = "Coral",
        primary = Color(0xFFFB7185),
        light = Color(0xFFFDA4AF),
        dark = Color(0xFFE11D48)
    );

    companion object {
        fun fromId(id: String?): AccentColorTheme {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: LIGHT_BLUE
        }
    }
}

val LocalAccentColor = staticCompositionLocalOf { AccentColorTheme.LIGHT_BLUE }

val LocalAccent: AccentColorTheme
    @Composable
    get() = LocalAccentColor.current

val AccentThemeColor: Color
    @Composable
    get() = LocalAccentColor.current.primary

// Typography
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xB8FFFFFF)
val TextTertiary = Color(0x70FFFFFF)
val TextDisabled = Color(0x38FFFFFF)

