package com.flowtap.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.flowtap.app.data.prefs.ThemeMode

/** Provides the active vs idle palette to every composable in the tree. */
val LocalFocusPalette = staticCompositionLocalOf<FocusPalette> { IdlePalette }

/** Resolves SYSTEM/LIGHT/DARK to a concrete dark flag. */
@Composable
fun rememberDarkTheme(themeMode: ThemeMode): Boolean {
    val systemDark = isSystemInDarkTheme()
    return when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> systemDark
    }
}

@Composable
fun FlowtapTheme(
    active: Boolean = false,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val palette = when {
        active && darkTheme -> ActivePalette
        active && !darkTheme -> ActiveLightPalette
        !active && !darkTheme -> IdleLightPalette
        else -> IdlePalette
    }
    val onAccent = if (darkTheme) Color(0xFF04121E) else Color.White
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = palette.accent,
            onPrimary = onAccent,
            primaryContainer = palette.card,
            onPrimaryContainer = palette.textPrimary,
            secondary = palette.accentText,
            onSecondary = onAccent,
            background = palette.background,
            onBackground = palette.textPrimary,
            surface = palette.card,
            onSurface = palette.textPrimary,
            surfaceVariant = palette.card,
            onSurfaceVariant = palette.textSecondary,
            surfaceContainer = palette.card,
            surfaceContainerLow = palette.card,
            outline = palette.cardBorder,
            outlineVariant = palette.ringTrack,
            error = palette.danger,
            onError = Color.White,
        )
    } else {
        lightColorScheme(
            primary = palette.accent,
            onPrimary = onAccent,
            primaryContainer = palette.card,
            onPrimaryContainer = palette.textPrimary,
            secondary = palette.accentText,
            onSecondary = onAccent,
            background = palette.background,
            onBackground = palette.textPrimary,
            surface = palette.card,
            onSurface = palette.textPrimary,
            surfaceVariant = palette.card,
            onSurfaceVariant = palette.textSecondary,
            surfaceContainer = palette.card,
            surfaceContainerLow = palette.card,
            outline = palette.cardBorder,
            outlineVariant = palette.ringTrack,
            error = palette.danger,
            onError = Color.White,
        )
    }
    CompositionLocalProvider(LocalFocusPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = NfcTypography,
            shapes = NfcShapes,
            content = content,
        )
    }
}
