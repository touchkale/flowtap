package com.flowtap.app.data.repo

import com.flowtap.app.data.model.SessionRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Derived statistics as reactive flows, computed by the pure [StatsCalculator].
 */
class StatsRepository(private val repository: FocusRepository) {

    private val sessions: Flow<List<SessionRecord>> = repository.sessions

    fun observeTodayTotalMs(): Flow<Long> = sessions.map { StatsCalculator.computeTodayTotalMs(it) }

    fun observeWeek(): Flow<WeekStats> = sessions.map { StatsCalculator.computeWeek(it) }

    fun observeStreakDays(): Flow<Int> = sessions.map { StatsCalculator.computeStreakDays(it) }

    fun observeLongestMs(): Flow<Long> = sessions.map { list ->
        list.filter { it.endedAt > 0 }.maxOfOrNull { it.durationMs } ?: 0L
    }

    fun observeAverageDailyMs(): Flow<Long> = sessions.map { StatsCalculator.computeAverageDaily(it) }

    fun observeRecent(limit: Int = 10): Flow<List<SessionRecord>> = sessions.map { list ->
        list.filter { it.endedAt > 0 }.take(limit)
    }
}