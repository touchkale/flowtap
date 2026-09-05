package com.flowtap.app.util

import org.junit.Assert.assertEquals
import org.junit.Test

class TimeUtilTest {

    @Test
    fun formatClockTime_standard() {
        assertEquals("01:37:42", TimeUtil.formatClockTime(1 * 3600_000L + 37 * 60_000L + 42_000L))
        assertEquals("00:00:00", TimeUtil.formatClockTime(0L))
        assertEquals("12:00:00", TimeUtil.formatClockTime(12 * 3600_000L))
        assertEquals("01:00:01", TimeUtil.formatClockTime(3600_000L + 1_000L))
    }

    @Test
    fun formatDuration_hoursMinutesSeconds() {
        assertEquals("1h 37m", TimeUtil.formatDuration(1 * 3600_000L + 37 * 60_000L))
        assertEquals("32m", TimeUtil.formatDuration(32 * 60_000L))
        assertEquals("45s", TimeUtil.formatDuration(45_000L))
        assertEquals("0s", TimeUtil.formatDuration(0L))
    }

    @Test
    fun formatHoursMinutes_roundsDown() {
        assertEquals("3h 42m", TimeUtil.formatHoursMinutes(3 * 3600_000L + 42 * 60_000L + 30_000L))
        assertEquals("32m", TimeUtil.formatHoursMinutes(32 * 60_000L))
        assertEquals("0m", TimeUtil.formatHoursMinutes(0L))
    }

    @Test
    fun startOfWeek_returnsMonday() {
        // 2026-09-02 is a Wednesday
        val wednesday = java.util.Calendar.getInstance().apply {
            clear()
            set(2026, java.util.Calendar.SEPTEMBER, 2, 12, 0, 0)
        }.timeInMillis
        val monday = java.util.Calendar.getInstance().apply {
            clear()
            set(2026, java.util.Calendar.AUGUST, 31, 0, 0, 0)
        }.timeInMillis
        assertEquals(monday, TimeUtil.startOfWeek(wednesday))
    }

    @Test
    fun localDayStart_stripsTime() {
        val input = java.util.Calendar.getInstance().apply {
            clear()
            set(2026, java.util.Calendar.SEPTEMBER, 2, 23, 59, 59)
        }.timeInMillis
        val midnight = java.util.Calendar.getInstance().apply {
            clear()
            set(2026, java.util.Calendar.SEPTEMBER, 2, 0, 0, 0)
        }.timeInMillis
        assertEquals(midnight, TimeUtil.localDayStart(input))
    }

    @Test
    fun weekdayInitials_order() {
        assertEquals("M", TimeUtil.weekdayInitial(0))
        assertEquals("S", TimeUtil.weekdayInitial(6))
        assertEquals("S", TimeUtil.weekdayInitial(7)) // clamped to last
    }
}