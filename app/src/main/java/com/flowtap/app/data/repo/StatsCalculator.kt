package com.flowtap.app.data.repo

import com.flowtap.app.data.model.DayTotal
import com.flowtap.app.data.model.SessionRecord
import com.flowtap.app.util.TimeUtil

data class WeekStats(
    val days: List<DayTotal>, // Monday..Sunday, current week
    val totalMs: Long,
)

/**
 * Pure statistics functions (no Android/DB dependencies) — unit-testable.
 * All take an explicit "now" so date-boundary behavior can be tested.
 */
object StatsCalculator {

    const val MIN_STREAK_DAY_MS = 60_000L

    /**
     * Sessions shorter than this are accidental taps: hidden from recents,
     * history and totals (kept in the DB, streak rule unchanged).
     */
    const val MIN_COUNTABLE_MS = 5 * 60_000L

    fun computeWeek(list: List<SessionRecord>, now: Long = System.currentTimeMillis()): WeekStats {
        val weekStart = TimeUtil.startOfWeek(now)
        val days = (0..6).map { i ->
            val dayStart = TimeUtil.addDays(weekStart, i)
            val dayEnd = TimeUtil.addDays(dayStart, 1)
            val total = list
                .filter { it.startedAt >= dayStart && it.startedAt < dayEnd }
                .filter { it.endedAt > 0 && it.durationMs >= MIN_COUNTABLE_MS }
                .sumOf { it.durationMs }
            DayTotal(dayStart = dayStart, totalMs = total)
        }
        return WeekStats(days = days, totalMs = days.sumOf { it.totalMs })
    }

    fun computeTodayTotalMs(
        list: List<SessionRecord>,
        now: Long = System.currentTimeMillis(),
    ): Long {
        val todayStart = TimeUtil.localDayStart(now)
        return list.filter {
            it.endedAt > 0 && it.durationMs >= MIN_COUNTABLE_MS && it.startedAt >= todayStart
        }.sumOf { it.durationMs }
    }

    /** Per-mode today totals (countable sessions only), for the Home selector. */
    fun computeTodayTotalByMode(
        list: List<SessionRecord>,
        now: Long = System.currentTimeMillis(),
    ): Map<Long, Long> {
        val todayStart = TimeUtil.localDayStart(now)
        return list.filter {
            it.endedAt > 0 && it.durationMs >= MIN_COUNTABLE_MS && it.startedAt >= todayStart
        }
            .groupBy { it.modeId }
            .mapValues { (_, sessions) -> sessions.sumOf { it.durationMs } }
    }

    fun computeStreakDays(list: List<SessionRecord>, now: Long = System.currentTimeMillis()): Int {
        val qualifying = list
            .filter { it.endedAt > 0 && it.durationMs >= MIN_STREAK_DAY_MS }
            .map { TimeUtil.localDayStart(it.startedAt) }
            .toSet()
        var count = 0
        var cursor = TimeUtil.localDayStart(now)
        if (cursor !in qualifying) cursor = TimeUtil.addDays(cursor, -1)
        while (cursor in qualifying) {
            count++
            cursor = TimeUtil.addDays(cursor, -1)
        }
        return count
    }

    /** Average daily deep work over the trailing 7 calendar days (including today). */
    fun computeAverageDaily(
        list: List<SessionRecord>,
        now: Long = System.currentTimeMillis(),
    ): Long {
        val start = TimeUtil.addDays(TimeUtil.localDayStart(now), -6)
        val total = list.filter {
            it.startedAt >= start && it.endedAt > 0 && it.durationMs >= MIN_COUNTABLE_MS
        }.sumOf { it.durationMs }
        return total / 7
    }
}