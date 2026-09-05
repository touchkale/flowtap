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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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

    val dailyGoalMs: StateFlow<Long> = settings.dailyGoalMinutes
        .map { it * 60_000L }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsDataStore.DEFAULT_DAILY_GOAL_MIN * 60_000L)

    val hardcoreMode: StateFlow<Boolean> = settings.hardcoreMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val recent: StateFlow<List<SessionRecord>> = stats.observeRecent(2)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val modes: StateFlow<List<FocusMode>> = repo.modes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** F13: the mode the Home button can soft-start — never a strict one. */
    val softStartMode: StateFlow<FocusMode?> = repo.modes
        .map { list -> list.firstOrNull { !it.strictMode } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val modeNames: StateFlow<Map<Long, String>> = repo.modes
        .map { list -> list.associate { it.id to it.name } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    /** Soft-start fallback for testing: starts the first NON-strict mode with no NFC tag. */
    fun startFocusDefault() {
        val mode = softStartMode.value ?: return
        SessionManager.startSession(mode, null)
    }

    /**
     * Home DND quick toggle. Flips the mode's stored DND flag (that session's
     * mode when running, else the default mode) and live-applies it to a
     * running session. The UI should open system DND settings when the result
     * is [DndToggleResult.ENABLED_WITHOUT_PERMISSION].
     */
    fun toggleModeDnd(onResult: (DndToggleResult) -> Unit) {
        viewModelScope.launch {
            val targetId = SessionManager.current?.modeId
                ?: modes.value.firstOrNull()?.id
            val mode = targetId?.let { repo.getMode(it) }
            if (mode == null) {
                onResult(DndToggleResult.NO_MODE)
                return@launch
            }
            val next = !mode.dndEnabled
            repo.updateMode(mode.copy(dndEnabled = next))
            if (SessionManager.current?.modeId == mode.id) {
                SessionManager.setSessionDnd(next)
            }
            onResult(if (next) DndToggleResult.ENABLED else DndToggleResult.DISABLED)
        }
    }

    fun endFocus() {
        SessionManager.endSession(EndReason.IN_APP)
    }

    /** Emergency exit: ends even strict/hardcore sessions (hold-to-end UI). */
    fun endFocusEmergency() {
        SessionManager.endSession(EndReason.EMERGENCY)
    }

    enum class DndToggleResult { ENABLED, DISABLED, NO_MODE }

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
