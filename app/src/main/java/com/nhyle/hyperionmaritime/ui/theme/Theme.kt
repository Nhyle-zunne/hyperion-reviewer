package com.nhyle.hyperionmaritime.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp

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
    val warning: Color,
    val isDark: Boolean = true
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
        warning = WarningAmber,
        isDark = true
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
    warning = WarningAmber,
    isDark = true
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
    warning = WarningAmber,
    isDark = false
)

val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF22C55E), // Accent Green #22C55E
    onPrimary = Color(0xFF0B1120),
    primaryContainer = Color(0xFF14532D),
    onPrimaryContainer = Color(0xFFDCFCE7),
    secondary = Color(0xFFF8FAFC),
    onSecondary = Color(0xFF0B1120),
    secondaryContainer = Color(0xFF334155),
    onSecondaryContainer = Color(0xFFF8FAFC),
    tertiary = Color(0xFF94A3B8),
    onTertiary = Color(0xFF0B1120),
    background = Color(0xFF0B1120), // Canvas #0B1120
    onBackground = Color(0xFFF8FAFC), // Text #F8FAFC
    surface = Color(0xFF1E293B), // Cards #1E293B
    onSurface = Color(0xFFF8FAFC), // Text #F8FAFC
    surfaceVariant = Color(0xFF334155),
    onSurfaceVariant = Color(0xFF94A3B8), // Muted Text #94A3B8
    error = Color(0xFFEF4444),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFEE2E2),
    outline = Color(0xFF334155),
    outlineVariant = Color(0xFF1E293B)
)

val LightColorScheme = lightColorScheme(
    primary = Color(0xFF15803D), // Accent Green #15803D
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDCFCE7),
    onPrimaryContainer = Color(0xFF14532D),
    secondary = Color(0xFF0F172A),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE2E8F0),
    onSecondaryContainer = Color(0xFF0F172A),
    tertiary = Color(0xFF475569),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFF1F5F9), // Canvas #F1F5F9
    onBackground = Color(0xFF0F172A), // Text #0F172A
    surface = Color(0xFFFFFFFF), // Cards #FFFFFF
    onSurface = Color(0xFF0F172A), // Text #0F172A
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF475569), // Muted Text #475569
    error = Color(0xFFDC2626),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D),
    outline = Color(0xFFE2E8F0), // Card Outline #E2E8F0
    outlineVariant = Color(0xFFCBD5E1)
)

val IndustrialShapes = Shapes(
    extraSmall = RoundedCornerShape(0.dp),
    small = RoundedCornerShape(0.dp),
    medium = RoundedCornerShape(0.dp),
    large = RoundedCornerShape(0.dp),
    extraLarge = RoundedCornerShape(0.dp)
)

@Composable
fun AppTheme(
    isDarkMode: Boolean = true, // Default to Dark Mode on first install
    content: @Composable () -> Unit
) {
    val colorScheme = if (isDarkMode) DarkColorScheme else LightColorScheme
    val spaghettiColors = if (isDarkMode) DarkSpaghettiColors else LightSpaghettiColors

    CompositionLocalProvider(LocalSpaghettiColors provides spaghettiColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = IndustrialShapes,
            content = content
        )
    }
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    AppTheme(isDarkMode = darkTheme, content = content)
}
