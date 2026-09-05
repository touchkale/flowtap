package com.flowtap.app.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.flowtap.app.data.model.ActiveSessionSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

/** App theme choice. SYSTEM follows the device night mode. */
enum class ThemeMode(val key: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark"),
    ;

    companion object {
        fun fromKey(key: String?): ThemeMode = entries.firstOrNull { it.key == key } ?: SYSTEM
    }
}

/** Central key-value store for settings and the persisted active-session snapshot. */
class SettingsDataStore(private val context: Context) {

    private object Keys {
        val DAILY_GOAL_MIN = intPreferencesKey("daily_goal_min")
        val WEEKLY_GOAL_MIN = intPreferencesKey("weekly_goal_min")
        val HARDCORE_MODE = booleanPreferencesKey("hardcore_mode")
        val DND_MASTER_ENABLED = booleanPreferencesKey("dnd_master_enabled")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val KEEP_SCREEN_AWAKE = booleanPreferencesKey("keep_screen_awake")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ACTIVE_SESSION_JSON = stringPreferencesKey("active_session_json")
        val PRIOR_DND_FILTER = intPreferencesKey("prior_dnd_filter")
        val PENDING_DND_RESTORE = booleanPreferencesKey("pending_dnd_restore")
    }

    val dailyGoalMinutes: Flow<Int> = context.settingsDataStore.data
        .map { it[Keys.DAILY_GOAL_MIN] ?: DEFAULT_DAILY_GOAL_MIN }

    val weeklyGoalMinutes: Flow<Int> = context.settingsDataStore.data
        .map { it[Keys.WEEKLY_GOAL_MIN] ?: DEFAULT_WEEKLY_GOAL_MIN }

    val hardcoreMode: Flow<Boolean> = context.settingsDataStore.data
        .map { it[Keys.HARDCORE_MODE] ?: false }

    /**
     * Universal DND switch: sessions only touch Do Not Disturb when this AND
     * the mode's own flag are both on. Defaults to true (previous behavior).
     */
    val dndMasterEnabled: Flow<Boolean> = context.settingsDataStore.data
        .map { it[Keys.DND_MASTER_ENABLED] ?: true }

    val onboardingDone: Flow<Boolean> = context.settingsDataStore.data
        .map { it[Keys.ONBOARDING_DONE] ?: false }

    val keepScreenAwake: Flow<Boolean> = context.settingsDataStore.data
        .map { it[Keys.KEEP_SCREEN_AWAKE] ?: false }

    val themeMode: Flow<ThemeMode> = context.settingsDataStore.data
        .map { ThemeMode.fromKey(it[Keys.THEME_MODE]) }

    val activeSession: Flow<ActiveSessionSnapshot?> = context.settingsDataStore.data
        .map { prefs -> prefs[Keys.ACTIVE_SESSION_JSON]?.let(::decodeSession) }

    val pendingDndRestore: Flow<Boolean> = context.settingsDataStore.data
        .map { it[Keys.PENDING_DND_RESTORE] ?: false }

    val priorDndFilter: Flow<Int?> = context.settingsDataStore.data
        .map { it[Keys.PRIOR_DND_FILTER] }

    suspend fun setDailyGoal(minutes: Int) {
        context.settingsDataStore.edit { it[Keys.DAILY_GOAL_MIN] = minutes }
    }

    suspend fun setWeeklyGoal(minutes: Int) {
        context.settingsDataStore.edit { it[Keys.WEEKLY_GOAL_MIN] = minutes }
    }

    suspend fun setHardcoreMode(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.HARDCORE_MODE] = enabled }
    }

    suspend fun setDndMasterEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.DND_MASTER_ENABLED] = enabled }
    }

    suspend fun setOnboardingDone() {
        context.settingsDataStore.edit { it[Keys.ONBOARDING_DONE] = true }
    }

    suspend fun setKeepScreenAwake(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.KEEP_SCREEN_AWAKE] = enabled }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { it[Keys.THEME_MODE] = mode.key }
    }

    suspend fun saveActiveSession(snapshot: ActiveSessionSnapshot) {
        context.settingsDataStore.edit { it[Keys.ACTIVE_SESSION_JSON] = encodeSession(snapshot) }
    }

    suspend fun clearActiveSession() {
        context.settingsDataStore.edit { it.remove(Keys.ACTIVE_SESSION_JSON) }
    }

    suspend fun savePriorDndFilter(filter: Int) {
        context.settingsDataStore.edit { it[Keys.PRIOR_DND_FILTER] = filter }
    }

    suspend fun clearPriorDndFilter() {
        context.settingsDataStore.edit { it.remove(Keys.PRIOR_DND_FILTER) }
    }

    suspend fun setPendingDndRestore(pending: Boolean) {
        context.settingsDataStore.edit { it[Keys.PENDING_DND_RESTORE] = pending }
    }

    private fun encodeSession(s: ActiveSessionSnapshot): String = JSONObject().apply {
        put("sessionId", s.sessionId)
        put("modeId", s.modeId)
        put("tagId", s.tagId ?: JSONObject.NULL)
        put("modeName", s.modeName)
        put("tagName", s.tagName ?: JSONObject.NULL)
        put("iconName", s.iconName)
        put("accentIndex", s.accentIndex)
        put("strictMode", s.strictMode)
        put("dndEnabled", s.dndEnabled)
        put("blockedApps", s.blockedApps.joinToString(","))
        put("startedAtEpoch", s.startedAtEpoch)
        if (s.keepAwake == null) put("keepAwake", JSONObject.NULL) else put("keepAwake", s.keepAwake)
        if (s.autoEndMinutes == null) put("autoEndMinutes", JSONObject.NULL) else put("autoEndMinutes", s.autoEndMinutes)
        put("allowlistOnly", s.allowlistOnly)
        put("allowedApps", s.allowedApps.joinToString(","))
    }.toString()

    private fun decodeSession(json: String): ActiveSessionSnapshot? = runCatching {
        val o = JSONObject(json)
        val blocked = o.optString("blockedApps", "")
            .split(",").filter { it.isNotBlank() }
        val allowed = o.optString("allowedApps", "")
            .split(",").filter { it.isNotBlank() }
        ActiveSessionSnapshot(
            sessionId = o.getLong("sessionId"),
            modeId = o.getLong("modeId"),
            tagId = if (o.isNull("tagId")) null else o.optLong("tagId"),
            modeName = o.getString("modeName"),
            tagName = if (o.isNull("tagName")) null else o.optString("tagName"),
            iconName = o.optString("iconName", "work"),
            accentIndex = o.optInt("accentIndex", 0),
            strictMode = o.optBoolean("strictMode", false),
            dndEnabled = o.optBoolean("dndEnabled", true),
            blockedApps = blocked,
            keepAwake = if (o.isNull("keepAwake")) null else o.optBoolean("keepAwake"),
            autoEndMinutes = if (o.isNull("autoEndMinutes")) null else o.optInt("autoEndMinutes").takeIf { it > 0 },
            allowlistOnly = o.optBoolean("allowlistOnly", false),
            allowedApps = allowed,
            startedAtEpoch = o.getLong("startedAtEpoch"),
        )
    }.getOrNull()

    companion object {
        const val DEFAULT_DAILY_GOAL_MIN = 300
        const val DEFAULT_WEEKLY_GOAL_MIN = 2100
    }
}
