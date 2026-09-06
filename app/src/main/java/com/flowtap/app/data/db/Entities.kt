package com.flowtap.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A focus mode profile (e.g. Deep Work, Reading, Workout).
 * blockedAppsJson is a JSON array of package names (see util/Json).
 * v2 adds: keepAwake (nullable override), autoEndMinutes (nullable),
 * allowlistOnly + allowedAppsJson, launchPackage.
 * v3 adds: dailyGoalMinutes (per-mode daily goal, default 300).
 */
@Entity(tableName = "modes")
data class ModeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val iconName: String,
    val accentIndex: Int,
    val dndEnabled: Boolean,
    val strictMode: Boolean,
    val blockedAppsJson: String,
    val keepAwake: Boolean? = null,
    val autoEndMinutes: Int? = null,
    val allowlistOnly: Boolean = false,
    val allowedAppsJson: String = "[]",
    val launchPackage: String? = null,
    val dailyGoalMinutes: Int = 300,
    val createdAt: Long,
)

/** A physical NFC tag bound to a mode. uidHex uniquely identifies the sticker. */
@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val uidHex: String,
    val modeId: Long,
    val createdAt: Long,
    val lastUsedAt: Long? = null,
)

/**
 * A completed (or in-progress) session.
 * An in-progress session has endedAt == 0 and durationMs == 0.
 * endReason values: tap_end | in_app | switched | timeout | reboot
 */
@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val modeId: Long,
    val tagId: Long? = null,
    val startedAt: Long,
    val endedAt: Long,
    val durationMs: Long,
    val endReason: String,
)
