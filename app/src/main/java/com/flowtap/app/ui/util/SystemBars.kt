package com.flowtap.app.ui.util

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Keeps status/navigation-bar icon contrast readable for the CURRENT Compose
 * theme. The app theme uses transparent system bars (edge-to-edge), and the
 * manifest theme can only express one static contrast — so every activity
 * calls this with its effective [darkTheme].
 */
@Composable
fun SystemBarAppearance(darkTheme: Boolean) {
    val view = LocalView.current
    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
    }
}
