package com.flowtap.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Design system palette — Direction B (deep navy + amber).
 * Idle: navy surfaces, amber brand accent. Active (session running): deep
 * green with an emerald "live" accent, so a running session still signals go.
 * Secondary text targets 4.5:1, primary 7:1 on their surfaces.
 * The coding model MUST use these exact hex values everywhere.
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
)

val IdlePalette = FocusPalette(
    background = Color(0xFF0A0E1A),
    card = Color(0xFF141B2E),
    cardBorder = Color(0xFF2A3552),
    accent = Color(0xFFF5A524),
    accentText = Color(0xFFFFC65C),
    ringTrack = Color(0xFF26314D),
    textPrimary = Color(0xFFF2F5FA),
    textSecondary = Color(0xFFA9B6CC),
    textTertiary = Color(0xFF6E7D99),
    danger = Color(0xFFE05B5B),
)

val ActivePalette = FocusPalette(
    background = Color(0xFF0A1410),
    card = Color(0xFF122019),
    cardBorder = Color(0xFF234034),
    accent = Color(0xFF34D399),
    accentText = Color(0xFF7DE8BE),
    ringTrack = Color(0xFF1E3A2F),
    textPrimary = Color(0xFFF0F7F3),
    textSecondary = Color(0xFFA6BFB4),
    textTertiary = Color(0xFF68867A),
    danger = Color(0xFFE05B5B),
)

// Light mode: warm paper + amber brand; active stays green.
val IdleLightPalette = FocusPalette(
    background = Color(0xFFF4F2EC),
    card = Color(0xFFFFFFFF),
    cardBorder = Color(0xFFE3D9C4),
    accent = Color(0xFFB45309),
    accentText = Color(0xFF92400E),
    ringTrack = Color(0xFFEADFC6),
    textPrimary = Color(0xFF16130D),
    textSecondary = Color(0xFF5B5546),
    textTertiary = Color(0xFF8A8172),
    danger = Color(0xFFC24848),
)

val ActiveLightPalette = FocusPalette(
    background = Color(0xFFEDF5EF),
    card = Color(0xFFFFFFFF),
    cardBorder = Color(0xFFCBE2D4),
    accent = Color(0xFF047857),
    accentText = Color(0xFF046C4E),
    ringTrack = Color(0xFFC2DCCF),
    textPrimary = Color(0xFF0A1A14),
    textSecondary = Color(0xFF3F5A4E),
    textTertiary = Color(0xFF779086),
    danger = Color(0xFFC24848),
)
