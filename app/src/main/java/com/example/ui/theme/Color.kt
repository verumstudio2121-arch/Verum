package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val DeepBlack = Color(0xFF000000)
val ObsidianBlack = Color(0xFF08080C)
val CharcoalGlass = Color(0xFF121218)

// Liquid Glass surfaces (iOS style Black #000000 at 20-30% opacity)
val GlassSurface = Color(0x3D000000)          // Black at ~24% opacity
val GlassSurfaceElevated = Color(0x48000000)  // Black at ~28% opacity
val GlassSurfaceSubtle = Color(0x33000000)    // Black at ~20% opacity
val GlassSelectedCapsule = Color(0x4D000000)  // Black at ~30% opacity

// Borders & Reflections (1px White #FFFFFF line at 15-20% opacity)
val GlassBorder = Color(0x2EFFFFFF)           // White at ~18% opacity (15-20% spec)
val GlassBorderBright = Color(0x4DFFFFFF)     // White at ~30% opacity for active/elevated
val GlassHighlight = Color(0x2BFFFFFF)

// Accents
val AccentCyan = Color(0xFF38BDF8)
val AccentOrange = Color(0xFFFF9F0A)
val AccentGreen = Color(0xFF34C759)
val AccentRed = Color(0xFFFF453A)
val AccentViolet = Color(0xFFA78BFA)

/**
 * Liquid Glass dynamic accent color contract.
 */
interface AccentTheme {
    val id: String
    val displayName: String
    val primary: Color
    val light: Color
    val dark: Color
}

data class DynamicAccentColor(
    override val id: String,
    override val displayName: String,
    override val primary: Color,
    override val light: Color,
    override val dark: Color
) : AccentTheme

/**
 * Liquid Glass dynamic accent color options.
 */
enum class AccentColorTheme(
    override val id: String,
    override val displayName: String,
    override val primary: Color,
    override val light: Color,
    override val dark: Color
) : AccentTheme {
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
        fun fromId(id: String?): AccentTheme {
            if (id == null) return LIGHT_BLUE
            val preset = entries.firstOrNull { it.id.equals(id, ignoreCase = true) }
            if (preset != null) return preset

            // If it's a custom hex color like "#FF5722" or "CUSTOM_#FF5722"
            if (id.startsWith("#") || id.startsWith("CUSTOM_#")) {
                try {
                    val hex = if (id.startsWith("CUSTOM_#")) id.removePrefix("CUSTOM_") else id
                    val parsedInt = android.graphics.Color.parseColor(hex)
                    val parsedColor = Color(parsedInt)
                    // Synthesize dynamic light/dark shades
                    val hsv = FloatArray(3)
                    android.graphics.Color.colorToHSV(parsedInt, hsv)

                    val lightHsv = floatArrayOf(
                        hsv[0],
                        (hsv[1] * 0.65f).coerceIn(0f, 1f),
                        (hsv[2] * 1.15f).coerceIn(0f, 1f)
                    )
                    val darkHsv = floatArrayOf(
                        hsv[0],
                        (hsv[1] * 1.25f).coerceIn(0f, 1f),
                        (hsv[2] * 0.75f).coerceIn(0f, 1f)
                    )

                    val lightColor = Color(android.graphics.Color.HSVToColor(lightHsv))
                    val darkColor = Color(android.graphics.Color.HSVToColor(darkHsv))

                    return DynamicAccentColor(
                        id = id,
                        displayName = "Custom",
                        primary = parsedColor,
                        light = lightColor,
                        dark = darkColor
                    )
                } catch (_: Exception) {
                    return LIGHT_BLUE
                }
            }

            return LIGHT_BLUE
        }
    }
}

val LocalAccentColor = staticCompositionLocalOf<AccentTheme> { AccentColorTheme.LIGHT_BLUE }

val LocalAccent: AccentTheme
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

