package com.flowtap.app.blocking

/**
 * Shared exemption policy for both blocking engines. Launcher, dialer and
 * essential system surfaces are never blocked, even in allow-only mode.
 * Device launchers/dialers are resolved at runtime; the static set covers
 * AOSP + common OEM call UI.
 */
object BlockExemptions {

    private val SYSTEM_EXEMPT = setOf(
        "com.android.systemui",
        "android",
    )

    private val CALL_EXEMPT = setOf(
        "com.android.server.telecom",
        "com.android.phone",
        "com.android.incallui",
        "com.google.android.dialer",
        "com.samsung.android.dialer",
        "com.samsung.android.incallui",
    )

    fun isExempt(pkg: String, ownPackage: String, homePackages: Set<String> = emptySet()): Boolean {
        if (pkg == ownPackage) return true
        if (pkg in SYSTEM_EXEMPT) return true
        if (pkg in CALL_EXEMPT) return true
        if (pkg in homePackages) return true
        return false
    }
}
