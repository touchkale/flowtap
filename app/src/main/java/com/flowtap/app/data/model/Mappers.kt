package com.flowtap.app.data.model

import com.flowtap.app.data.db.ModeEntity
import com.flowtap.app.data.db.SessionEntity
import com.flowtap.app.data.db.TagEntity
import com.flowtap.app.util.Json

fun ModeEntity.toDomain(): FocusMode = FocusMode(
    id = id,
    name = name,
    iconName = iconName,
    accentIndex = accentIndex,
    dndEnabled = dndEnabled,
    strictMode = strictMode,
    blockedApps = Json.decodeStringList(blockedAppsJson),
    keepAwake = keepAwake,
    autoEndMinutes = autoEndMinutes,
    allowlistOnly = allowlistOnly,
    allowedApps = runCatching { Json.decodeStringList(allowedAppsJson) }.getOrDefault(emptyList()),
    launchPackage = launchPackage?.takeIf { it.isNotBlank() },
    createdAt = createdAt,
)

fun FocusMode.toEntity(): ModeEntity = ModeEntity(
    id = id,
    name = name,
    iconName = iconName,
    accentIndex = accentIndex,
    dndEnabled = dndEnabled,
    strictMode = strictMode,
    blockedAppsJson = Json.encodeStringList(blockedApps),
    keepAwake = keepAwake,
    autoEndMinutes = autoEndMinutes,
    allowlistOnly = allowlistOnly,
    allowedAppsJson = Json.encodeStringList(allowedApps),
    launchPackage = launchPackage?.takeIf { it.isNotBlank() },
    createdAt = createdAt,
)

fun TagEntity.toDomain(): FocusTag = FocusTag(
    id = id,
    name = name,
    uidHex = uidHex,
    modeId = modeId,
    createdAt = createdAt,
    lastUsedAt = lastUsedAt,
)

fun FocusTag.toEntity(): TagEntity = TagEntity(
    id = id,
    name = name,
    uidHex = uidHex,
    modeId = modeId,
    createdAt = createdAt,
    lastUsedAt = lastUsedAt,
)

fun SessionEntity.toDomain(): SessionRecord = SessionRecord(
    id = id,
    modeId = modeId,
    tagId = tagId,
    startedAt = startedAt,
    endedAt = endedAt,
    durationMs = durationMs,
    endReason = EndReason.from(endReason),
)

fun SessionRecord.toEntity(): SessionEntity = SessionEntity(
    id = id,
    modeId = modeId,
    tagId = tagId,
    startedAt = startedAt,
    endedAt = endedAt,
    durationMs = durationMs,
    endReason = endReason.value,
)
