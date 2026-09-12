package com.flowtap.app.blocking

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.SystemClock
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import com.flowtap.app.session.SessionManager
import com.flowtap.app.util.Haptics

/**
 * Watches the foreground app. When a blocked app is opened during a focus
 * session, it immediately sends the user home and shows the block overlay.
 *
 * F6: the overlay is drawn as a TYPE_ACCESSIBILITY_OVERLAY window (immune to
 * background-activity-start restrictions); BlockOverlayActivity is only a
 * fallback if the window cannot be attached.
 *
 * Ignores: our own package, system UI, and duplicate events within the
 * debounce window. Blocked set is read live from SessionManager.
 */
class FocusAccessibilityService : AccessibilityService() {

    private val lastBlockedAt = HashMap<String, Long>()
    private var overlay: BlockOverlayController? = null
    private var lastOverlayShownAt = 0L
    private var homePackages: Set<String>? = null

    /** Launcher + critical system surfaces are never blocked, even in allowlist mode. */
    private fun isExempt(pkg: String): Boolean {
        val homes = homePackages ?: run {
            val resolved = runCatching {
                val home = android.content.Intent(android.content.Intent.ACTION_MAIN)
                    .addCategory(android.content.Intent.CATEGORY_HOME)
                packageManager.queryIntentActivities(home, 0)
                    .map { it.activityInfo.packageName }.toSet()
            }.getOrDefault(emptySet())
            homePackages = resolved
            resolved
        }
        return BlockExemptions.isExempt(pkg, packageName, homes)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        isConnected = true
        overlay = BlockOverlayController(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        // S2: only the selected engine enforces. When the user chose
        // compatibility, accessibility stays silent even if still enabled.
        if (com.flowtap.app.session.EngineState.blockingMethod !=
            com.flowtap.app.data.prefs.BlockingMethod.FULL_POWER
        ) {
            overlay?.dismiss()
            return
        }

        val snap = SessionManager.current
        if (snap == null) {
            overlay?.dismiss()
            return
        }

        val pkg = event.packageName?.toString() ?: return
        val now = SystemClock.elapsedRealtime()
        if (isExempt(pkg)) {
            if (overlay?.isShowing == true && now - lastOverlayShownAt > NAV_DISMISS_GRACE_MS) {
                overlay?.dismiss()
            }
            return
        }

        // The user navigated somewhere else while the overlay is up (home,
        // recents, an incoming call, any allowed surface): follow the gesture
        // and get the overlay out of the way. The session keeps running —
        // reopening a blocked app re-shows it. The grace window ignores the
        // launcher event caused by our own HOME action right after showing.
        if (!snap.isBlocked(pkg, packageName)) {
            if (overlay?.isShowing == true && now - lastOverlayShownAt > NAV_DISMISS_GRACE_MS) {
                overlay?.dismiss()
            }
            return
        }

        if (now - (lastBlockedAt[pkg] ?: 0L) < BLOCK_DEBOUNCE_MS) return
        lastBlockedAt[pkg] = now

        performGlobalAction(GLOBAL_ACTION_HOME)
        Haptics.tick(this)

        // Primary: accessibility overlay window. Fallback: overlay activity.
        if (overlay?.show(snap, pkg) != true) {
            runCatching {
                val intent = Intent(this, BlockOverlayActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                startActivity(intent)
            }
        } else {
            lastOverlayShownAt = now
        }
    }

    /**
     * The fullscreen overlay window consumes touches but the BACK key would
     * otherwise fall through to whatever is underneath (usually the launcher,
     * which ignores it) — feeling completely dead. While the overlay is up,
     * BACK takes the user to their timer instead. Requires
     * canRequestFilterKeyEvents in the service config.
     */
    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
            if (overlay?.handleBack() == true) return true
        }
        return super.onKeyEvent(event)
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        isConnected = false
        overlay?.destroy()
        overlay = null
        super.onDestroy()
    }

    companion object {
        private const val BLOCK_DEBOUNCE_MS = 1500L
        private const val NAV_DISMISS_GRACE_MS = 1500L

        /** Actual service connection, distinct from the settings permission flag (S3). */
        @Volatile
        var isConnected: Boolean = false
            private set
    }
}