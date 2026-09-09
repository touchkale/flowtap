package com.flowtap.app.data.repo

import com.flowtap.app.data.model.SessionRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/**
 * Derived statistics as reactive flows, computed by the pure [StatsCalculator].
 *
 * Date-boundary staleness (T2): the flows recompute on session writes AND on
 * [refreshTime] (resume/day-boundary), so crossing midnight without a new
 * session still refreshes today's totals instead of going stale.
 */
class StatsRepository(private val repository: FocusRepository) {

    private val sessions: Flow<List<SessionRecord>> = repository.sessions

    private val timeRefresh = MutableStateFlow(0L)

    /** Re-evaluates all time-dependent stats with the current clock. */
    fun refreshTime() {
        timeRefresh.value = System.currentTimeMillis()
    }

    private fun <T> withSessionsAndTime(block: (List<SessionRecord>) -> T): Flow<T> =
        combine(sessions, timeRefresh) { list, _ -> block(list) }

    fun observeTodayTotalMs(): Flow<Long> =
        withSessionsAndTime { StatsCalculator.computeTodayTotalMs(it) }

    fun observeTodayTotalByMode(): Flow<Map<Long, Long>> =
        withSessionsAndTime { StatsCalculator.computeTodayTotalByMode(it) }

    fun observeWeek(): Flow<WeekStats> =
        withSessionsAndTime { StatsCalculator.computeWeek(it) }

    fun observeStreakDays(): Flow<Int> =
        withSessionsAndTime { StatsCalculator.computeStreakDays(it) }

    fun observeLongestMs(): Flow<Long> = sessions.map { list ->
        list.filter { it.endedAt > 0 }.maxOfOrNull { it.durationMs } ?: 0L
    }

    fun observeAverageDailyMs(): Flow<Long> =
        withSessionsAndTime { StatsCalculator.computeAverageDaily(it) }

    fun observeRecent(limit: Int = 10): Flow<List<SessionRecord>> = sessions.map { list ->
        list.filter { it.endedAt > 0 }
            .take(limit)
    }

    /** This week's totals for one mode (all completed sessions). */
    fun observeWeekForMode(modeId: Long): Flow<WeekStats> =
        withSessionsAndTime { list ->
            StatsCalculator.computeWeek(list.filter { it.modeId == modeId })
        }
}
