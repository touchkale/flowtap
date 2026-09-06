package com.flowtap.app.data.model

/**
 * Immutable snapshot of the running session. Persisted to DataStore so the
 * app can survive process death and reboot reconciliation.
 */
data class ActiveSessionSnapshot(
    val sessionId: Long,
    val modeId: Long,
    val tagId: Long? = null,
    val modeName: String,
    val tagName: String? = null,
    val iconName: String,
    val accentIndex: Int = 0,
    val strictMode: Boolean = false,
    val notificationMode: NotificationMode = NotificationMode.SILENT,
    val blockedApps: List<String> = emptyList(),
    /** Nullable keep-awake override captured from the mode; null = follow global. */
    val keepAwake: Boolean? = null,
    /** Auto-end after N minutes; null = unlimited (12h hard cap still applies). */
    val autoEndMinutes: Int? = null,
    val allowlistOnly: Boolean = false,
    val allowedApps: List<String> = emptyList(),
    val startedAtEpoch: Long,
) {
    fun elapsedMs(now: Long = System.currentTimeMillis()): Long =
        (now - startedAtEpoch).coerceAtLeast(0L)

    /** True when [pkg] must be blocked under this session's enforcement mode. */
    fun isBlocked(pkg: String, ownPackage: String): Boolean {
        if (pkg == ownPackage) return false
        if (pkg == "com.android.systemui" || pkg == "android") return false
        return if (allowlistOnly) {
            pkg !in allowedApps
        } else {
            pkg in blockedApps
        }
    }

    /** Remaining ms before the per-mode auto-end fires; null = unlimited. */
    fun autoEndRemainingMs(now: Long = System.currentTimeMillis()): Long? {
        val mins = autoEndMinutes ?: return null
        return (mins * 60_000L - (now - startedAtEpoch)).coerceAtLeast(0L)
    }
}
