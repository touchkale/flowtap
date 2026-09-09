package com.flowtap.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flowtap.app.data.model.EndReason
import com.flowtap.app.data.model.FocusMode
import com.flowtap.app.data.model.SessionRecord
import com.flowtap.app.data.prefs.SettingsDataStore
import com.flowtap.app.data.repo.FocusRepository
import com.flowtap.app.data.repo.StatsRepository
import com.flowtap.app.di.AppContainer
import com.flowtap.app.session.SessionManager
import com.flowtap.app.ui.vmFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(
    private val repo: FocusRepository,
    stats: StatsRepository,
    settings: SettingsDataStore,
) : ViewModel() {

    val todayTotalMs: StateFlow<Long> = stats.observeTodayTotalMs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val todayByMode: StateFlow<Map<Long, Long>> = stats.observeTodayTotalByMode()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // Kept for compat: global default is no longer used for progress (per-mode goals win).
    // Exposed so old callers don't break during migration.
    val dailyGoalMs: StateFlow<Long> = settings.dailyGoalMinutes
        .map { it * 60_000L }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsDataStore.DEFAULT_DAILY_GOAL_MIN * 60_000L)

    val hardcoreMode: StateFlow<Boolean> = settings.hardcoreMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val recent: StateFlow<List<SessionRecord>> = stats.observeRecent(2)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val modes: StateFlow<List<FocusMode>> = repo.modes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Dropdown selection for idle preview. Null = first mode. Reset when modes change. */
    private val _selectedModeId = MutableStateFlow<Long?>(null)
    val selectedModeId: StateFlow<Long?> = _selectedModeId

    val selectedMode: StateFlow<FocusMode?> = combine(modes, _selectedModeId) { list, sel ->
        if (list.isEmpty()) null
        else list.firstOrNull { it.id == sel } ?: list.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val selectedTodayMs: StateFlow<Long> = combine(todayByMode, selectedMode) { byMode, mode ->
        if (mode == null) 0L else byMode[mode.id] ?: 0L
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val selectedGoalMs: StateFlow<Long> = selectedMode
        .map { (it?.dailyGoalMinutes ?: SettingsDataStore.DEFAULT_DAILY_GOAL_MIN) * 60_000L }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsDataStore.DEFAULT_DAILY_GOAL_MIN * 60_000L)

    fun selectMode(id: Long) {
        _selectedModeId.value = id
    }

    /** F13: the mode the Home button can soft-start — the selected non-strict mode. */
    val softStartMode: StateFlow<FocusMode?> = selectedMode
        .map { mode -> mode?.takeIf { !it.strictMode } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val modeNames: StateFlow<Map<Long, String>> = repo.modes
        .map { list -> list.associate { it.id to it.name } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    /** Soft-start fallback for testing: starts the selected NON-strict mode with no NFC tag. */
    fun startFocusDefault() {
        val mode = softStartMode.value ?: return
        SessionManager.startSession(mode, null)
    }

    /**
     * Home notification quick toggle. Cycles the mode's behavior
     * None → Silent → DND (that session's mode when running, else the
     * selected mode) and live-applies it to a running session. The UI should
     * open system DND settings when the result is DND without permission.
     */
    fun cycleNotificationMode(onResult: (CycleResult) -> Unit) {
        viewModelScope.launch {
            val targetId = SessionManager.current?.modeId
                ?: selectedMode.value?.id
                ?: modes.value.firstOrNull()?.id
            val mode = targetId?.let { repo.getMode(it) }
            if (mode == null) {
                onResult(CycleResult.NO_MODE)
                return@launch
            }
            val next = when (mode.notificationMode) {
                com.flowtap.app.data.model.NotificationMode.NONE ->
                    com.flowtap.app.data.model.NotificationMode.SILENT
                com.flowtap.app.data.model.NotificationMode.SILENT ->
                    com.flowtap.app.data.model.NotificationMode.DND
                com.flowtap.app.data.model.NotificationMode.DND ->
                    com.flowtap.app.data.model.NotificationMode.NONE
            }
            repo.updateMode(mode.copy(notificationMode = next))
            if (SessionManager.current?.modeId == mode.id) {
                SessionManager.setSessionNotificationMode(next)
            }
            onResult(
                when (next) {
                    com.flowtap.app.data.model.NotificationMode.DND -> CycleResult.DND
                    com.flowtap.app.data.model.NotificationMode.SILENT -> CycleResult.SILENT
                    com.flowtap.app.data.model.NotificationMode.NONE -> CycleResult.NONE
                }
            )
        }
    }

    /**
     * Direct sound selection (H2): the Home chip is a true three-option
     * selector, not a switch. Same persistence + live-apply as the cycle.
     */
    fun setNotificationMode(
        next: com.flowtap.app.data.model.NotificationMode,
        onResult: (CycleResult) -> Unit,
    ) {
        viewModelScope.launch {
            val targetId = SessionManager.current?.modeId
                ?: selectedMode.value?.id
                ?: modes.value.firstOrNull()?.id
            val mode = targetId?.let { repo.getMode(it) }
            if (mode == null) {
                onResult(CycleResult.NO_MODE)
                return@launch
            }
            if (mode.notificationMode != next) {
                repo.updateMode(mode.copy(notificationMode = next))
                if (SessionManager.current?.modeId == mode.id) {
                    SessionManager.setSessionNotificationMode(next)
                }
            }
            onResult(
                when (next) {
                    com.flowtap.app.data.model.NotificationMode.DND -> CycleResult.DND
                    com.flowtap.app.data.model.NotificationMode.SILENT -> CycleResult.SILENT
                    com.flowtap.app.data.model.NotificationMode.NONE -> CycleResult.NONE
                }
            )
        }
    }

    fun endFocus() {
        SessionManager.endSession(EndReason.IN_APP)
    }

    /** Emergency exit: ends even strict/hardcore sessions (hold-to-end UI). */
    fun endFocusEmergency() {
        SessionManager.endSession(EndReason.EMERGENCY)
    }

    enum class CycleResult { DND, SILENT, NONE, NO_MODE }

    companion object {
        fun factory(container: AppContainer) = vmFactory {
            HomeViewModel(
                container.focusRepository,
                container.statsRepository,
                container.settingsDataStore,
            )
        }
    }
}
