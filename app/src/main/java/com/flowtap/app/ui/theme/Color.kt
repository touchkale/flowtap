package com.flowtap.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Design system palette.
 * Idle theme (Home / History / Settings): dark slate blue.
 * Active theme (session running): dark teal.
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
    background = Color(0xFF0B0F14),
    card = Color(0xFF161D26),
    cardBorder = Color(0xFF232C37),
    accent = Color(0xFF5B84AC),
    accentText = Color(0xFF7FA3C4),
    ringTrack = Color(0xFF24303C),
    textPrimary = Color(0xFFE8EEF4),
    textSecondary = Color(0xFF93A1B0),
    textTertiary = Color(0xFF5C6B7A),
    danger = Color(0xFFE05B5B),
)

val ActivePalette = FocusPalette(
    background = Color(0xFF0A1411),
    card = Color(0xFF12211D),
    cardBorder = Color(0xFF1C332E),
    accent = Color(0xFF2DBDA4),
    accentText = Color(0xFF7BD8C8),
    ringTrack = Color(0xFF1C332E),
    textPrimary = Color(0xFFE4F2EE),
    textSecondary = Color(0xFF8FA8A1),
    textTertiary = Color(0xFF5C756E),
    danger = Color(0xFFE05B5B),
)

// Light mode: same hue family, tuned for contrast on light surfaces.
val IdleLightPalette = FocusPalette(
    background = Color(0xFFF3F6F9),
    card = Color(0xFFFFFFFF),
    cardBorder = Color(0xFFE1E8F0),
    accent = Color(0xFF35678F),
    accentText = Color(0xFF2E5D84),
    ringTrack = Color(0xFFD9E3EC),
    textPrimary = Color(0xFF0F1B26),
    textSecondary = Color(0xFF4B5D6E),
    textTertiary = Color(0xFF7D8FA0),
    danger = Color(0xFFC24848),
)

val ActiveLightPalette = FocusPalette(
    background = Color(0xFFEBF4F1),
    card = Color(0xFFFFFFFF),
    cardBorder = Color(0xFFD2E4DD),
    accent = Color(0xFF0B8F7A),
    accentText = Color(0xFF0A7666),
    ringTrack = Color(0xFFC8E0D8),
    textPrimary = Color(0xFF0A1B16),
    textSecondary = Color(0xFF47645C),
    textTertiary = Color(0xFF7B968D),
    danger = Color(0xFFC24848),
)
