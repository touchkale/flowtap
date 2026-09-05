package com.flowtap.app.blocking

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flowtap.app.MainActivity
import com.flowtap.app.FlowtapApp
import com.flowtap.app.data.model.EndReason
import com.flowtap.app.session.SessionManager
import com.flowtap.app.ui.theme.FlowtapTheme
import com.flowtap.app.ui.theme.rememberDarkTheme

/**
 * Fallback fullscreen overlay shown when a blocked app is opened during a
 * session (used only if the accessibility overlay window fails to attach).
 * It disappears automatically when the session ends, and always offers an
 * explicit way out (End session for flexible sessions, Show timer otherwise).
 */
class BlockOverlayActivity : ComponentActivity() {

    private fun openTimer() {
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        )
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Back closes the overlay to the home screen (the blocking service
        // sends HOME before launching this fallback, so finishing lands home —
        // it never reveals the blocked app underneath or loop-traps).
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    finish()
                }
            },
        )
        setContent {
            val snap by SessionManager.active.collectAsStateWithLifecycle()
            LaunchedEffect(snap) {
                if (snap == null) finish()
            }
            val current = snap
            val app = application as FlowtapApp
            val themeMode by app.container.settingsDataStore.themeMode
                .collectAsStateWithLifecycle(
                    initialValue = com.flowtap.app.data.prefs.ThemeMode.SYSTEM,
                )
            val darkTheme = rememberDarkTheme(themeMode)
            com.flowtap.app.ui.util.SystemBarAppearance(darkTheme)
            FlowtapTheme(active = true, darkTheme = darkTheme) {
                if (current != null) {
                    BlockOverlayContent(
                        snap = current,
                        onOpenTimer = { openTimer() },
                        onEndSession = if (current.strictMode) {
                            null
                        } else {
                            { SessionManager.endSession(EndReason.IN_APP) }
                        },
                        onEmergencyEnd = if (current.strictMode) {
                            { SessionManager.endSession(EndReason.EMERGENCY) }
                        } else {
                            null
                        },
                    )
                }
            }
        }
    }
}