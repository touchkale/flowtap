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
     * All completed sessions count toward totals/history. Previously sessions
     * under five minutes were hidden, which looked like data loss (T1).
     * Kept for reference; no longer used as a filter.
     */
    const val MIN_COUNTABLE_MS = 5 * 60_000L

    /** Overlap of [startedAt, endedAt) with [rangeStart, rangeEnd), in ms. */
    fun overlapMs(startedAt: Long, endedAt: Long, rangeStart: Long, rangeEnd: Long): Long {
        val start = maxOf(startedAt, rangeStart)
        val end = minOf(endedAt, rangeEnd)
        return (end - start).coerceAtLeast(0L)
    }

    fun computeWeek(list: List<SessionRecord>, now: Long = System.currentTimeMillis()): WeekStats {
        val weekStart = TimeUtil.startOfWeek(now)
        val days = (0..6).map { i ->
            val dayStart = TimeUtil.addDays(weekStart, i)
            val dayEnd = TimeUtil.addDays(dayStart, 1)
            val total = list
                .filter { it.endedAt > 0 }
                .sumOf { overlapMs(it.startedAt, it.endedAt, dayStart, dayEnd) }
            DayTotal(dayStart = dayStart, totalMs = total)
        }
        return WeekStats(days = days, totalMs = days.sumOf { it.totalMs })
    }

    fun computeTodayTotalMs(
        list: List<SessionRecord>,
        now: Long = System.currentTimeMillis(),
    ): Long {
        val todayStart = TimeUtil.localDayStart(now)
        val tomorrowStart = TimeUtil.addDays(todayStart, 1)
        return list.filter { it.endedAt > 0 }
            .sumOf { overlapMs(it.startedAt, it.endedAt, todayStart, tomorrowStart) }
    }

    /** Per-mode today totals (all completed sessions), for the Home selector. */
    fun computeTodayTotalByMode(
        list: List<SessionRecord>,
        now: Long = System.currentTimeMillis(),
    ): Map<Long, Long> {
        val todayStart = TimeUtil.localDayStart(now)
        val tomorrowStart = TimeUtil.addDays(todayStart, 1)
        return list.filter { it.endedAt > 0 }
            .groupBy { it.modeId }
            .mapValues { (_, sessions) ->
                sessions.sumOf { overlapMs(it.startedAt, it.endedAt, todayStart, tomorrowStart) }
            }
            .filterValues { it > 0 }
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
        val end = TimeUtil.addDays(TimeUtil.localDayStart(now), 1)
        val total = list.filter { it.endedAt > 0 }
            .sumOf { overlapMs(it.startedAt, it.endedAt, start, end) }
        return total / 7
    }
}