package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = MaritimeGoldBright,
    onPrimary = NauticalNavyDark,
    primaryContainer = NauticalSurfaceVariantDark,
    onPrimaryContainer = MaritimeGoldLight,
    secondary = SeaCyan,
    onSecondary = NauticalNavyDark,
    secondaryContainer = SeaCyanContainer,
    onSecondaryContainer = PureWhite,
    tertiary = PassGreen,
    onTertiary = NauticalNavyDark,
    background = NauticalNavyDark,
    onBackground = PureWhite,
    surface = NauticalSurfaceDark,
    onSurface = PureWhite,
    surfaceVariant = NauticalSurfaceVariantDark,
    onSurfaceVariant = TextSecondaryDark,
    error = FailRed,
    onError = PureWhite,
    errorContainer = FailRedContainer,
    onErrorContainer = FailRedText,
    outline = NauticalCardBorderDark
)

private val LightColorScheme = lightColorScheme(
    primary = MaritimeGold,
    onPrimary = NauticalNavyDark,
    primaryContainer = Color(0xFFFEF3C7),
    onPrimaryContainer = Color(0xFF78350F),
    secondary = SeaCyanDark,
    onSecondary = PureWhite,
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = Color(0xFF0369A1),
    tertiary = PassGreen,
    onTertiary = PureWhite,
    background = NauticalNavyLight,
    onBackground = NauticalNavyDark,
    surface = NauticalSurfaceLight,
    onSurface = NauticalNavyDark,
    surfaceVariant = NauticalSurfaceVariantLight,
    onSurfaceVariant = TextSecondaryLight,
    error = FailRed,
    onError = PureWhite,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = FailRedContainer,
    outline = NauticalCardBorderLight
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
