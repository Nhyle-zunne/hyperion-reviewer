package com.nhyle.hyperionmaritime.ui.theme

import androidx.compose.ui.graphics.Color

// ===================================================
// THEME COLOR TOKENS (LIGHT & DARK SYSTEM)
// ===================================================

// Light Theme Tokens
val CanvasLight = Color(0xFFF1F5F9) // Canvas #F1F5F9
val CardLight = Color(0xFFFFFFFF) // Cards #FFFFFF
val CardOutlineLight = Color(0xFFCBD5E1) // Card Outline #CBD5E1
val TextLight = Color(0xFF0F172A) // Text #0F172A
val TextMutedLight = Color(0xFF475569) // Muted Text #475569
val AccentGreenLight = Color(0xFF15803D) // Accent Green #15803D

val SpaghettiLightBackground = CanvasLight
val SpaghettiLightSurface = CardLight
val SpaghettiLightSurface2 = Color(0xFFE2E8F0)
val SpaghettiLightForeground = TextLight
val SpaghettiLightMuted = TextMutedLight
val SpaghettiLightBorder = CardOutlineLight
val SpaghettiLightInput = Color(0xFFCBD5E1)
val SpaghettiLightPrimary = AccentGreenLight
val SpaghettiLightPrimaryForeground = Color(0xFFFFFFFF)
val SpaghettiLightSecondary = CardOutlineLight
val SpaghettiLightSecondaryForeground = TextLight
val SpaghettiLightAccent = AccentGreenLight
val SpaghettiLightAccentForeground = Color(0xFFFFFFFF)
val SpaghettiLightRing = AccentGreenLight

// Dark Theme Tokens
val CanvasDark = Color(0xFF0B1120) // Canvas #0B1120
val CardDark = Color(0xFF1E293B) // Cards #1E293B
val CardOutlineDark = Color(0xFF334155) // Border #334155
val TextDark = Color(0xFFF8FAFC) // Text #F8FAFC
val TextMutedDark = Color(0xFF94A3B8) // Muted Text #94A3B8
val AccentGreenDark = Color(0xFF22C55E) // Accent Green #22C55E

val SpaghettiDarkBackground = CanvasDark
val SpaghettiDarkSurface = CardDark
val SpaghettiDarkSurface2 = Color(0xFF334155)
val SpaghettiDarkForeground = TextDark
val SpaghettiDarkMuted = TextMutedDark
val SpaghettiDarkBorder = CardOutlineDark
val SpaghettiDarkInput = Color(0xFF334155)
val SpaghettiDarkPrimary = AccentGreenDark
val SpaghettiDarkPrimaryForeground = Color(0xFF0B1120)
val SpaghettiDarkSecondary = CardDark
val SpaghettiDarkSecondaryForeground = TextDark
val SpaghettiDarkAccent = AccentGreenDark
val SpaghettiDarkAccentForeground = Color(0xFFFFFFFF)
val SpaghettiDarkRing = AccentGreenDark

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
