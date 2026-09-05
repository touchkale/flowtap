package com.flowtap.app

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flowtap.app.data.model.EndReason
import com.flowtap.app.session.SessionManager
import com.flowtap.app.ui.navigation.MainApp
import com.flowtap.app.ui.theme.FlowtapTheme
import com.flowtap.app.util.Notifications
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Bottom navigation tabs. Labels resolve via [labelRes] for localization. */
enum class MainTab(
    val labelRes: Int,
    val icon: ImageVector,
    val iconSelected: ImageVector,
) {
    HOME(R.string.nav_home, Icons.Outlined.Home, Icons.Filled.Home),
    HISTORY(R.string.nav_history, Icons.Outlined.History, Icons.Filled.History),
    TAGS(R.string.nav_tags, Icons.Outlined.Sell, Icons.Filled.Sell),
    MODES(R.string.nav_modes, Icons.Outlined.Tune, Icons.Filled.Tune),
    SETTINGS(R.string.nav_settings, Icons.Outlined.Settings, Icons.Filled.Settings),
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleAction(intent)
        enableEdgeToEdge()
        setContent {
            val sessionActive by SessionManager.active.collectAsStateWithLifecycle()
            val container = (application as FlowtapApp).container
            val keepAwakeGlobal by container.settingsDataStore.keepScreenAwake
                .collectAsStateWithLifecycle(initialValue = false)
            val themeMode by container.settingsDataStore.themeMode
                .collectAsStateWithLifecycle(initialValue = com.flowtap.app.data.prefs.ThemeMode.SYSTEM)
            val darkTheme = com.flowtap.app.ui.theme.rememberDarkTheme(themeMode)
            com.flowtap.app.ui.util.SystemBarAppearance(darkTheme)

            LaunchedEffect(keepAwakeGlobal, sessionActive) {
                // Per-mode override wins; null follows the global setting.
                val effectiveKeepAwake = sessionActive != null &&
                    (sessionActive!!.keepAwake ?: keepAwakeGlobal)
                if (effectiveKeepAwake) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }

            FlowtapTheme(
                active = sessionActive != null,
                darkTheme = darkTheme,
            ) {
                MainApp()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleAction(intent)
    }

    private fun handleAction(intent: Intent?) {
        val app = application as FlowtapApp
        when (intent?.getStringExtra(Notifications.EXTRA_ACTION)) {
            Notifications.ACTION_END_SESSION -> SessionManager.endSession(EndReason.IN_APP)
            ACTION_START_FOCUS -> app.appScope.launch {
                // Launcher shortcut: soft-start the first non-strict mode.
                // Graceful no-ops: active session exists, or strict-only setup.
                SessionManager.ensureLoaded()
                if (SessionManager.current != null) return@launch
                val mode = app.container.focusRepository.modes.first()
                    .firstOrNull { !it.strictMode } ?: return@launch
                SessionManager.startSession(mode, null)
            }
        }
    }

    companion object {
        const val ACTION_START_FOCUS = "start_focus"
    }
}
