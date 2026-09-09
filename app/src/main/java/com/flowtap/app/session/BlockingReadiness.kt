package com.flowtap.app.session

import com.flowtap.app.data.prefs.BlockingMethod

/**
 * Shared readiness model for Home + Settings. A configured permission is
 * not proof of active enforcement — permission, selected method, running
 * engine and live session are modeled separately.
 */
data class BlockingReadiness(
    val method: BlockingMethod,
    val accessibilityEnabled: Boolean,
    val accessibilityServiceConnected: Boolean,
    val usageAccessGranted: Boolean,
    val overlayGranted: Boolean,
    val sessionActive: Boolean,
    val selectedAppCount: Int,
) {
    /** The engine that will actually enforce, given the current selection. */
    val effectiveEngine: BlockingMethod = method

    /** True when the selected engine can enforce right now. */
    val isReady: Boolean = when (method) {
        BlockingMethod.FULL_POWER -> accessibilityEnabled && accessibilityServiceConnected
        BlockingMethod.COMPATIBILITY -> usageAccessGranted && overlayGranted
    }

    /** True when both engines could operate (switching left accessibility on). */
    val hasEngineConflict: Boolean =
        method == BlockingMethod.COMPATIBILITY && accessibilityEnabled

    /** Human-readable missing requirement, or null when ready. */
    fun missingRequirement(): String? {
        if (isReady) return null
        return when (method) {
            BlockingMethod.FULL_POWER -> if (!accessibilityEnabled) {
                "accessibility"
            } else {
                "service-connection"
            }
            BlockingMethod.COMPATIBILITY -> when {
                !usageAccessGranted -> "usage-access"
                !overlayGranted -> "overlay"
                else -> null
            }
        }
    }

    /** Status line that never claims "On" for a mere permission grant. */
    fun statusLabel(): String = when {
        !sessionActive && !isReady -> "Action needed"
        sessionActive && !isReady -> "Timer only — blocking unavailable"
        sessionActive && isReady -> "Blocking active"
        else -> "Ready"
    }
}
