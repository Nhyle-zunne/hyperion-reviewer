package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class SpaghettiColors(
    val background: Color,
    val surface: Color,
    val surface2: Color,
    val foreground: Color,
    val muted: Color,
    val border: Color,
    val input: Color,
    val primary: Color,
    val primaryForeground: Color,
    val secondary: Color,
    val secondaryForeground: Color,
    val accent: Color,
    val accentForeground: Color,
    val ring: Color,
    val success: Color,
    val error: Color,
    val warning: Color
)

val LocalSpaghettiColors = staticCompositionLocalOf {
    SpaghettiColors(
        background = SpaghettiDarkBackground,
        surface = SpaghettiDarkSurface,
        surface2 = SpaghettiDarkSurface2,
        foreground = SpaghettiDarkForeground,
        muted = SpaghettiDarkMuted,
        border = SpaghettiDarkBorder,
        input = SpaghettiDarkInput,
        primary = SpaghettiDarkPrimary,
        primaryForeground = SpaghettiDarkPrimaryForeground,
        secondary = SpaghettiDarkSecondary,
        secondaryForeground = SpaghettiDarkSecondaryForeground,
        accent = SpaghettiDarkAccent,
        accentForeground = SpaghettiDarkAccentForeground,
        ring = SpaghettiDarkRing,
        success = PassGreen,
        error = FailRed,
        warning = WarningAmber
    )
}

private val DarkSpaghettiColors = SpaghettiColors(
    background = SpaghettiDarkBackground,
    surface = SpaghettiDarkSurface,
    surface2 = SpaghettiDarkSurface2,
    foreground = SpaghettiDarkForeground,
    muted = SpaghettiDarkMuted,
    border = SpaghettiDarkBorder,
    input = SpaghettiDarkInput,
    primary = SpaghettiDarkPrimary,
    primaryForeground = SpaghettiDarkPrimaryForeground,
    secondary = SpaghettiDarkSecondary,
    secondaryForeground = SpaghettiDarkSecondaryForeground,
    accent = SpaghettiDarkAccent,
    accentForeground = SpaghettiDarkAccentForeground,
    ring = SpaghettiDarkRing,
    success = PassGreen,
    error = FailRed,
    warning = WarningAmber
)

private val LightSpaghettiColors = SpaghettiColors(
    background = SpaghettiLightBackground,
    surface = SpaghettiLightSurface,
    surface2 = SpaghettiLightSurface2,
    foreground = SpaghettiLightForeground,
    muted = SpaghettiLightMuted,
    border = SpaghettiLightBorder,
    input = SpaghettiLightInput,
    primary = SpaghettiLightPrimary,
    primaryForeground = SpaghettiLightPrimaryForeground,
    secondary = SpaghettiLightSecondary,
    secondaryForeground = SpaghettiLightSecondaryForeground,
    accent = SpaghettiLightAccent,
    accentForeground = SpaghettiLightAccentForeground,
    ring = SpaghettiLightRing,
    success = PassGreenDark,
    error = FailRedDark,
    warning = WarningAmber
)

private val SpaghettiDarkColorScheme = darkColorScheme(
    primary = SpaghettiDarkAccent, // Single Tomato Red Accent #FF5A67
    onPrimary = SpaghettiDarkAccentForeground,
    primaryContainer = Color(0xFF241416),
    onPrimaryContainer = SpaghettiDarkAccent,
    secondary = SpaghettiDarkForeground,
    onSecondary = SpaghettiDarkPrimaryForeground,
    secondaryContainer = SpaghettiDarkSurface2,
    onSecondaryContainer = SpaghettiDarkForeground,
    tertiary = SpaghettiDarkMuted,
    onTertiary = SpaghettiDarkBackground,
    background = SpaghettiDarkBackground, // #0C0C0C
    onBackground = SpaghettiDarkForeground, // #F2F2F0
    surface = SpaghettiDarkSurface, // #141414
    onSurface = SpaghettiDarkForeground, // #F2F2F0
    surfaceVariant = SpaghettiDarkSurface2, // #1D1D1D
    onSurfaceVariant = SpaghettiDarkMuted, // #A3A3A0
    error = SpaghettiDarkAccent,
    onError = SpaghettiDarkPrimaryForeground,
    errorContainer = Color(0xFF2B1416),
    onErrorContainer = SpaghettiDarkAccent,
    outline = SpaghettiDarkBorder, // #2A2A2A
    outlineVariant = SpaghettiDarkBorder
)

private val SpaghettiLightColorScheme = lightColorScheme(
    primary = SpaghettiLightAccent, // Single Tomato Red Accent #C8001A
    onPrimary = SpaghettiLightAccentForeground,
    primaryContainer = Color(0xFFFDE8EA),
    onPrimaryContainer = SpaghettiLightAccent,
    secondary = SpaghettiLightForeground,
    onSecondary = SpaghettiLightPrimaryForeground,
    secondaryContainer = SpaghettiLightSurface2,
    onSecondaryContainer = SpaghettiLightForeground,
    tertiary = SpaghettiLightMuted,
    onTertiary = SpaghettiLightBackground,
    background = SpaghettiLightBackground, // #FFFFFF
    onBackground = SpaghettiLightForeground, // #111111
    surface = SpaghettiLightSurface, // #FFFFFF
    onSurface = SpaghettiLightForeground, // #111111
    surfaceVariant = SpaghettiLightSurface2, // #F4F4F2
    onSurfaceVariant = SpaghettiLightMuted, // #5C5C5C
    error = SpaghettiLightAccent,
    onError = SpaghettiLightPrimaryForeground,
    errorContainer = Color(0xFFFDE8EA),
    onErrorContainer = SpaghettiLightAccent,
    outline = SpaghettiLightBorder, // #E4E4E1
    outlineVariant = SpaghettiLightBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to deep Italian poster black background
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) SpaghettiDarkColorScheme else SpaghettiLightColorScheme
    val spaghettiColors = if (darkTheme) DarkSpaghettiColors else LightSpaghettiColors

    CompositionLocalProvider(LocalSpaghettiColors provides spaghettiColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
