package com.flowtap.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flowtap.app.data.model.FocusMode
import com.flowtap.app.data.model.SessionRecord
import com.flowtap.app.data.prefs.SettingsDataStore
import com.flowtap.app.data.repo.FocusRepository
import com.flowtap.app.data.repo.StatsCalculator
import com.flowtap.app.data.repo.StatsRepository
import com.flowtap.app.data.repo.WeekStats
import com.flowtap.app.di.AppContainer
import com.flowtap.app.ui.vmFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(
    stats: StatsRepository,
    private val repo: FocusRepository,
    settings: SettingsDataStore,
) : ViewModel() {

    val week: StateFlow<WeekStats> = stats.observeWeek()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WeekStats(emptyList(), 0L))

    val modes: StateFlow<List<FocusMode>> = repo.modes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Per-mode weeks: each mode against its own 7× daily target. */
    val modeWeeks: StateFlow<Map<Long, WeekStats>> =
        combine(repo.sessions, repo.modes) { sessions, modes ->
            modes.associate { mode ->
                mode.id to StatsCalculator.computeWeek(
                    sessions.filter { it.modeId == mode.id },
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    /** Combined daily goal across modes, for the daily-bars chart line. */
    val combinedDailyMs: StateFlow<Long> = repo.modes
        .map { list ->
            val sumMin = list.sumOf { it.dailyGoalMinutes }
                .takeIf { it > 0 } ?: SettingsDataStore.DEFAULT_DAILY_GOAL_MIN
            sumMin * 60_000L
        }
        .stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000),
            SettingsDataStore.DEFAULT_DAILY_GOAL_MIN * 60_000L,
        )

    /** Weekly target is derived: 7 x sum(per-mode daily goals). No global UI. */
    val weeklyGoalMs: StateFlow<Long> = repo.modes
        .map { list ->
            val dailySumMin = if (list.isEmpty()) SettingsDataStore.DEFAULT_DAILY_GOAL_MIN
            else list.sumOf { it.dailyGoalMinutes }
            dailySumMin * 7 * 60_000L
        }
        .stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000),
            SettingsDataStore.DEFAULT_WEEKLY_GOAL_MIN * 60_000L,
        )

    val streakDays: StateFlow<Int> = stats.observeStreakDays()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val longestMs: StateFlow<Long> = stats.observeLongestMs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val averageDailyMs: StateFlow<Long> = stats.observeAverageDailyMs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val sessions: StateFlow<List<SessionRecord>> = repo.sessions
        .map { list ->
            list.filter {
                it.endedAt > 0 && // F4: never show in-progress rows
                    it.durationMs >= com.flowtap.app.data.repo.StatsCalculator.MIN_COUNTABLE_MS
            }
        } // F4: never show in-progress rows
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val modeNames: StateFlow<Map<Long, String>> = repo.modes
        .map { list -> list.associate { it.id to it.name } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun deleteSession(id: Long) {
        viewModelScope.launch { repo.deleteSession(id) }
    }

    companion object {
        fun factory(container: AppContainer) = vmFactory {
            HistoryViewModel(
                container.statsRepository,
                container.focusRepository,
                container.settingsDataStore,
            )
        }
    }
}