package com.flowtap.app.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object TimeUtil {

    fun localDayStart(epochMs: Long): Long {
        val c = Calendar.getInstance()
        c.timeInMillis = epochMs
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    fun addDays(epochMs: Long, days: Int): Long {
        val c = Calendar.getInstance()
        c.timeInMillis = epochMs
        c.add(Calendar.DAY_OF_YEAR, days)
        return c.timeInMillis
    }

    /** Monday 00:00 local of the week containing epochMs. */
    fun startOfWeek(epochMs: Long): Long {
        val c = Calendar.getInstance()
        c.timeInMillis = localDayStart(epochMs)
        val dayOfWeek = c.get(Calendar.DAY_OF_WEEK) // SUNDAY=1 .. SATURDAY=7
        val offset = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - Calendar.MONDAY
        c.add(Calendar.DAY_OF_YEAR, -offset)
        return c.timeInMillis
    }

    /** "3h 42m" or "32m" or "45s". */
    fun formatDuration(ms: Long): String {
        val totalSec = ms / 1000
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        return when {
            h > 0 -> "${h}h ${m}m"
            m > 0 -> "${m}m"
            else -> "${s}s"
        }
    }

    /** "4h 18m" style with hours, else "32m". */
    fun formatHoursMinutes(ms: Long): String {
        val totalMin = ms / 60000
        val h = totalMin / 60
        val m = totalMin % 60
        return if (h > 0) "${h}h ${m}m" else "${m}m"
    }

    /** Goal display: "5h" when whole hours, else "3h 30m". */
    fun formatGoal(minutes: Long): String {
        val h = minutes / 60
        val m = minutes % 60
        return if (m == 0L) "${h}h" else "${h}h ${m}m"
    }

    /** "10:32 AM" */
    fun formatClock(epochMs: Long): String =
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(epochMs))

    /** "01:37:42" live timer format. */
    fun formatClockTime(ms: Long): String {
        val totalSec = ms / 1000
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        return "%02d:%02d:%02d".format(h, m, s)
    }

    /** "7:15 AM — 2h 05m" used on session rows. */
    fun formatSessionRow(startedAt: Long, durationMs: Long): String =
        "${formatClock(startedAt)} — ${formatDuration(durationMs)}"

    /** "Apr 28 – May 4" for the weekly chart header. */
    fun formatWeekRange(weekStart: Long): String {
        val fmt = SimpleDateFormat("MMM d", Locale.getDefault())
        return "${fmt.format(Date(weekStart))} – ${fmt.format(Date(addDays(weekStart, 6)))}"
    }

    /** "Apr 28" */
    fun formatDay(epochMs: Long): String =
        SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(epochMs))

    private val weekdayInitials = listOf("M", "T", "W", "T", "F", "S", "S")
    fun weekdayInitial(index: Int): String = weekdayInitials[index.coerceIn(0, 6)]
}
