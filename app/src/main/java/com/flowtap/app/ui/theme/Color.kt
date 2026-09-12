package com.flowtap.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Warm paper system from docs/workflows/DESIGN.md.
 * Light + Dark only. A running session keeps the same background and uses a
 * subtle live accent (status label + ring progress), not a separate theme.
 */
data class FocusPalette(
    val background: Color,
    val card: Color,
    val cardBorder: Color,
    val accent: Color,
    val accentText: Color,
    val ringTrack: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val danger: Color,
    val selectedSurface: Color,
    val onAccent: Color,
)

val LightPalette = FocusPalette(
    background = Color(0xFFF5F4EE),
    card = Color(0xFFFFFFFF),
    cardBorder = Color(0xFFDCE3DC),
    accent = Color(0xFF245C48),
    accentText = Color(0xFF245C48),
    ringTrack = Color(0xFFDCE3DC),
    textPrimary = Color(0xFF192D28),
    textSecondary = Color(0xFF596A63),
    textTertiary = Color(0xFF75867E),
    danger = Color(0xFFA43D35),
    selectedSurface = Color(0xFFE4EDE3),
    onAccent = Color(0xFFFFFFFF),
)

val DarkPalette = FocusPalette(
    background = Color(0xFF101B18),
    card = Color(0xFF1B2A24),
    cardBorder = Color(0xFF34473C),
    accent = Color(0xFFACD6B0),
    accentText = Color(0xFFACD6B0),
    ringTrack = Color(0xFF34473C),
    textPrimary = Color(0xFFF0F4ED),
    textSecondary = Color(0xFFB0C2B6),
    textTertiary = Color(0xFF8AA396),
    danger = Color(0xFFFFB4AA),
    selectedSurface = Color(0xFF2B4234),
    onAccent = Color(0xFF142B1F),
)

// Compatibility aliases: the idle/active split is retired. Both point at the
// same paper palettes so older references keep compiling.
val IdlePalette = DarkPalette
val ActivePalette = DarkPalette
val IdleLightPalette = LightPalette
val ActiveLightPalette = LightPalette
