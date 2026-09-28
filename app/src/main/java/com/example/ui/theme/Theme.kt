package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF38BDF8),
    onPrimary = Color(0xFF0A0F1D),
    primaryContainer = Color(0xFF1B2A47),
    onPrimaryContainer = Color(0xFF38BDF8),
    secondary = SeaCyan,
    onSecondary = Color(0xFF0A0F1D),
    secondaryContainer = SeaCyanContainer,
    onSecondaryContainer = PureWhite,
    tertiary = Color(0xFFFBBF24),
    onTertiary = Color(0xFF0F172A),
    background = Color(0xFF0A0F1D),
    onBackground = Color(0xFFF1F5F9),
    surface = Color(0xFF131D31),
    onSurface = Color(0xFFE2E8F0),
    surfaceVariant = Color(0xFF1B2A47),
    onSurfaceVariant = Color(0xFF94A3B8),
    error = FailRed,
    onError = PureWhite,
    errorContainer = FailRedContainer,
    onErrorContainer = FailRedText,
    outline = Color(0xFF2B4468)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0284C7),
    onPrimary = PureWhite,
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = Color(0xFF0369A1),
    secondary = Color(0xFF0284C7),
    onSecondary = PureWhite,
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = Color(0xFF0369A1),
    tertiary = Color(0xFF0F172A),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFF1F5F9),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1E293B),
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF475569),
    error = FailRed,
    onError = PureWhite,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = FailRedContainer,
    outline = Color(0xFFCBD5E1)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to deep Nautical Navy for authentic maritime bridge display
    dynamicColor: Boolean = false, // Keep authentic maritime identity
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
