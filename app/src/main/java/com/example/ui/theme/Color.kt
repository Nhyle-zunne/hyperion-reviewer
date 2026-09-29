package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ===================================================
// MIAN "SPAGHETTI" ITALIAN POSTER DESIGN TOKENS
// ===================================================

// Light Theme Tokens
val SpaghettiLightBackground = Color(0xFFFFFFFF)
val SpaghettiLightSurface = Color(0xFFFFFFFF)
val SpaghettiLightSurface2 = Color(0xFFF4F4F2)
val SpaghettiLightForeground = Color(0xFF111111)
val SpaghettiLightMuted = Color(0xFF5C5C5C)
val SpaghettiLightBorder = Color(0xFFE4E4E1)
val SpaghettiLightInput = Color(0xFFCFCFCB)
val SpaghettiLightPrimary = Color(0xFF111111)
val SpaghettiLightPrimaryForeground = Color(0xFFFFFFFF)
val SpaghettiLightSecondary = Color(0xFFF4F4F2)
val SpaghettiLightSecondaryForeground = Color(0xFF111111)
val SpaghettiLightAccent = Color(0xFFC8001A) // Light Tomato Red
val SpaghettiLightAccentForeground = Color(0xFFFFFFFF)
val SpaghettiLightRing = Color(0xFFC8001A)

// Dark Theme Tokens
val SpaghettiDarkBackground = Color(0xFF0C0C0C)
val SpaghettiDarkSurface = Color(0xFF141414)
val SpaghettiDarkSurface2 = Color(0xFF1D1D1D)
val SpaghettiDarkForeground = Color(0xFFF2F2F0)
val SpaghettiDarkMuted = Color(0xFFA3A3A0)
val SpaghettiDarkBorder = Color(0xFF2A2A2A)
val SpaghettiDarkInput = Color(0xFF444444)
val SpaghettiDarkPrimary = Color(0xFFF2F2F0)
val SpaghettiDarkPrimaryForeground = Color(0xFF111111)
val SpaghettiDarkSecondary = Color(0xFF1D1D1D)
val SpaghettiDarkSecondaryForeground = Color(0xFFF2F2F0)
val SpaghettiDarkAccent = Color(0xFFFF5A67) // Dark Tomato Red
val SpaghettiDarkAccentForeground = Color(0xFF1A0003)
val SpaghettiDarkRing = Color(0xFFFF5A67)

// Common default tokens (pointing to Dark mode as default examination reviewer theme)
val MianBg = SpaghettiDarkBackground
val MianSurface = SpaghettiDarkSurface
val MianSurfaceVariant = SpaghettiDarkSurface2
val MianInk = SpaghettiDarkForeground
val MianMuted = SpaghettiDarkMuted
val MianRule = SpaghettiDarkBorder
val MianAccent = SpaghettiDarkAccent // #FF5A67

// Functional / Semantic feedback tokens (for scoring, pass/fail, exam diagnostics)
val PassGreen = Color(0xFF4ADE80)
val PassGreenDark = Color(0xFF16A34A)
val PassGreenContainer = Color(0xFF152A1C)
val PassGreenText = Color(0xFF4ADE80)

val FailRed = Color(0xFFFF5A67)
val FailRedDark = Color(0xFFDC2626)
val FailRedContainer = Color(0xFF2B1416)
val FailRedText = Color(0xFFFF5A67)

val WarningAmber = Color(0xFFFBBF24)
val WarningContainer = Color(0xFF2E2412)

// Legacy aliases mapped directly to Spaghetti tokens to eliminate old nautical identity
val NauticalNavyDark = SpaghettiDarkBackground
val NauticalSurfaceDark = SpaghettiDarkSurface
val NauticalSurfaceVariantDark = SpaghettiDarkSurface2
val NauticalCardBorderDark = SpaghettiDarkBorder

val NauticalNavyLight = SpaghettiLightBackground
val NauticalSurfaceLight = SpaghettiLightSurface
val NauticalSurfaceVariantLight = SpaghettiLightSurface2
val NauticalCardBorderLight = SpaghettiLightBorder

val MaritimeGold = SpaghettiDarkForeground
val MaritimeGoldBright = SpaghettiDarkForeground
val MaritimeGoldLight = SpaghettiDarkMuted
val MaritimeGoldContainer = SpaghettiDarkSurface2

val SeaCyan = SpaghettiDarkMuted
val SeaCyanDark = SpaghettiDarkBorder
val SeaCyanContainer = SpaghettiDarkSurface2

val PureWhite = SpaghettiDarkForeground
val TextSecondaryDark = SpaghettiDarkMuted
val TextSecondaryLight = SpaghettiLightMuted
