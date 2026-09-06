package com.flowtap.app.session

/**
 * Pure foreground-app decision logic for compatibility blocking mode.
 * No Android dependencies — fully unit-testable.
 *
 * Compatibility mode polls UsageStatsManager every 2s instead of using the
 * accessibility service (for users who refuse accessibility or hit the
 * UPI/banking conflict). Given timestamped foreground events, returns the
 * package that was most recently moved to foreground within [windowMs].
 */
object UsageBlockPoller {
    data class ForegroundEvent(val packageName: String, val timeMs: Long)

    /**
     * Returns the current foreground package, or null when no event falls
     * inside the window (stale data — never guess).
     */
    fun currentPackage(
        events: List<ForegroundEvent>,
        nowMs: Long,
        windowMs: Long = 5000L,
    ): String? {
        if (events.isEmpty()) return null
        return events
            .filter { it.timeMs <= nowMs && nowMs - it.timeMs <= windowMs }
            .maxByOrNull { it.timeMs }
            ?.packageName
    }

    /**
     * True when [pkg] must be blocked under this session's enforcement mode.
     * Mirrors ActiveSessionSnapshot.isBlocked minus the own-package/system
     * exemptions (those are applied by the caller with live PackageManager data).
     */
    fun isBlocked(
        pkg: String,
        blockedApps: List<String>,
        allowlistOnly: Boolean,
        allowedApps: List<String>,
    ): Boolean = if (allowlistOnly) {
        pkg !in allowedApps
    } else {
        pkg in blockedApps
    }
}
