package com.flowtap.app.data.repo

import com.flowtap.app.data.model.SessionRecord
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class StatsCalculatorTest {

    private fun day(month: Int, dayOfMonth: Int, hour: Int = 12, minute: Int = 0): Long =
        Calendar.getInstance().apply {
            clear()
            set(2026, month, dayOfMonth, hour, minute, 0)
        }.timeInMillis

    private fun session(startedAt: Long, durationMs: Long = 60 * 60_000L, ended: Boolean = true): SessionRecord =
        SessionRecord(
            modeId = 1,
            startedAt = startedAt,
            endedAt = if (ended) startedAt + durationMs else 0,
            durationMs = if (ended) durationMs else 0,
        )

    @Test
    fun computeTodayTotal_onlyCountsTodayAndCompleted() {
        val today = day(Calendar.SEPTEMBER, 2, 9)
        val now = day(Calendar.SEPTEMBER, 2, 18)
        val list = listOf(
            session(today, 60 * 60_000L),
            session(day(Calendar.SEPTEMBER, 1, 10), 60 * 60_000L),
            session(today, 120 * 60_000L, ended = false), // in-progress excluded
        )
        assertEquals(60 * 60_000L, StatsCalculator.computeTodayTotalMs(list, now))
    }

    @Test
    fun computeWeek_usesMondayStart() {
        // 2026-09-02 is Wednesday; week starts 2026-08-31 (Monday)
        val monday = day(Calendar.AUGUST, 31, 0)
        val wednesday = day(Calendar.SEPTEMBER, 2, 9)
        val sunday = day(Calendar.SEPTEMBER, 6, 9)
        val nextMonday = day(Calendar.SEPTEMBER, 7, 9)
        val now = day(Calendar.SEPTEMBER, 2, 18)

        val week = StatsCalculator.computeWeek(
            listOf(
                session(monday, 60 * 60_000L),
                session(wednesday, 30 * 60_000L),
                session(nextMonday, 60 * 60_000L), // next week — excluded
            ),
            now,
        )
        assertEquals(7, week.days.size)
        assertEquals(monday, week.days.first().dayStart)
        assertEquals(90 * 60_000L, week.totalMs)
        assertEquals(60 * 60_000L, week.days[0].totalMs) // Monday bucket
        assertEquals(30 * 60_000L, week.days[2].totalMs) // Wednesday bucket
        assertEquals(0L, week.days[6].totalMs) // Sunday empty
    }

    @Test
    fun computeStreak_consecutiveDaysBackFromToday() {
        val now = day(Calendar.SEPTEMBER, 2, 20)
        val today = day(Calendar.SEPTEMBER, 2, 9)
        val yesterday = day(Calendar.SEPTEMBER, 1, 9)
        val twoDaysAgo = day(Calendar.AUGUST, 31, 9)
        val fourDaysAgo = day(Calendar.AUGUST, 29, 9)

        val list = listOf(
            session(today),
            session(yesterday),
            session(twoDaysAgo),
            session(fourDaysAgo), // gap — stops the streak
        )
        assertEquals(3, StatsCalculator.computeStreakDays(list, now))
    }

    @Test
    fun computeStreak_startsFromYesterdayWhenTodayEmpty() {
        val now = day(Calendar.SEPTEMBER, 2, 20)
        val yesterday = day(Calendar.SEPTEMBER, 1, 9)
        val twoDaysAgo = day(Calendar.AUGUST, 31, 9)

        val list = listOf(session(yesterday), session(twoDaysAgo))
        assertEquals(2, StatsCalculator.computeStreakDays(list, now))
    }

    @Test
    fun computeStreak_ignoresSubMinuteSessions() {
        val now = day(Calendar.SEPTEMBER, 2, 20)
        val today = day(Calendar.SEPTEMBER, 2, 9)
        val list = listOf(session(today, durationMs = 30_000L))
        assertEquals(0, StatsCalculator.computeStreakDays(list, now))
    }

    @Test
    fun computeAverageDaily_trailingSevenDays() {
        val now = day(Calendar.SEPTEMBER, 2, 20)
        val sixDaysAgo = day(Calendar.AUGUST, 27, 9)
        val tenDaysAgo = day(Calendar.AUGUST, 23, 9)
        val list = listOf(
            session(now, 7 * 60 * 60_000L), // 7h today
            session(sixDaysAgo, 7 * 60 * 60_000L), // 7h six days ago
            session(tenDaysAgo, 70 * 60 * 60_000L), // outside window — excluded
        )
        assertEquals(2 * 60 * 60_000L, StatsCalculator.computeAverageDaily(list, now))
    }

    @Test
    fun computeWeek_emptyData() {
        val week = StatsCalculator.computeWeek(emptyList())
        assertEquals(7, week.days.size)
        assertEquals(0L, week.totalMs)
    }
}