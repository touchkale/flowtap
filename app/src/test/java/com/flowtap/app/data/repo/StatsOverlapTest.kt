package com.flowtap.app.data.repo

import com.flowtap.app.data.model.SessionRecord
import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Test

class StatsOverlapTest {

    private fun at(month: Int, day: Int, hour: Int, minute: Int): Long =
        Calendar.getInstance().apply {
            clear()
            set(2026, month, day, hour, minute, 0)
        }.timeInMillis

    private fun rec(startedAt: Long, endedAt: Long, modeId: Long = 1) = SessionRecord(
        modeId = modeId,
        startedAt = startedAt,
        endedAt = endedAt,
        durationMs = (endedAt - startedAt).coerceAtLeast(0L),
    )

    @Test
    fun midnightSession_splitsAcrossDays() {
        // 23:50 -> 00:10 across Sep 2 -> Sep 3.
        val start = at(Calendar.SEPTEMBER, 2, 23, 50)
        val end = at(Calendar.SEPTEMBER, 3, 0, 10)
        val list = listOf(rec(start, end))

        val sep2Evening = at(Calendar.SEPTEMBER, 2, 18, 0)
        val sep3Morning = at(Calendar.SEPTEMBER, 3, 9, 0)
        assertEquals(10 * 60_000L, StatsCalculator.computeTodayTotalMs(list, sep2Evening))
        assertEquals(10 * 60_000L, StatsCalculator.computeTodayTotalMs(list, sep3Morning))

        val week = StatsCalculator.computeWeek(list, sep3Morning)
        val total = week.days.sumOf { it.totalMs }
        assertEquals(20 * 60_000L, total)
    }

    @Test
    fun shortSessions_areRetained() {
        val start = at(Calendar.SEPTEMBER, 2, 9, 0)
        val oneMin = rec(start, start + 60_000L)
        val now = at(Calendar.SEPTEMBER, 2, 18, 0)
        assertEquals(60_000L, StatsCalculator.computeTodayTotalMs(listOf(oneMin), now))
    }

    @Test
    fun averageDaily_usesOverlapWindow() {
        val start = at(Calendar.SEPTEMBER, 2, 23, 50)
        val end = at(Calendar.SEPTEMBER, 3, 0, 10)
        val now = at(Calendar.SEPTEMBER, 3, 9, 0)
        val avg = StatsCalculator.computeAverageDaily(listOf(rec(start, end)), now)
        // 20 minutes across 7 days.
        assertEquals((20 * 60_000L) / 7, avg)
    }

    @Test
    fun overlapMs_clamps() {
        assertEquals(0L, StatsCalculator.overlapMs(100, 200, 200, 300))
        assertEquals(50L, StatsCalculator.overlapMs(100, 200, 150, 300))
        assertEquals(100L, StatsCalculator.overlapMs(100, 200, 0, 500))
    }
}
