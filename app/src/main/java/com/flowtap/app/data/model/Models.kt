package com.flowtap.app.data.model

enum class EndReason(val value: String) {
    TAP_END("tap_end"),
    IN_APP("in_app"),
    SWITCHED("switched"),
    TIMEOUT("timeout"),
    EMERGENCY("emergency"),
    REBOOT("reboot");

    companion object {
        fun from(value: String): EndReason =
            entries.firstOrNull { it.value == value } ?: TAP_END
    }
}

data class FocusMode(
    val id: Long = 0,
    val name: String,
    val iconName: String,
    val accentIndex: Int = 0,
    val dndEnabled: Boolean = true,
    val strictMode: Boolean = false,
    val blockedApps: List<String> = emptyList(),
    /** Null = follow global keep-awake setting; true/false overrides it for this mode. */
    val keepAwake: Boolean? = null,
    /** Auto-end after N minutes; null = unlimited. */
    val autoEndMinutes: Int? = null,
    /** When true, only [allowedApps] (+ system) are usable; everything else is blocked. */
    val allowlistOnly: Boolean = false,
    val allowedApps: List<String> = emptyList(),
    /** Package launched automatically when a session of this mode starts; null = none. */
    val launchPackage: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

data class FocusTag(
    val id: Long = 0,
    val name: String,
    val uidHex: String,
    val modeId: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val lastUsedAt: Long? = null,
)

data class SessionRecord(
    val id: Long = 0,
    val modeId: Long,
    val tagId: Long? = null,
    val startedAt: Long,
    val endedAt: Long,
    val durationMs: Long,
    val endReason: EndReason = EndReason.TAP_END,
)

/** One day bucket used by the weekly chart / stats. */
data class DayTotal(
    val dayStart: Long, // local midnight epoch ms
    val totalMs: Long,
)
