package com.flowtap.app.util

/**
 * Single source of truth for the emergency hold-to-end interaction (blocking
 * overlay + Home lock box). The countdown math is pure so it is unit-tested;
 * both UIs just render [progress]/[remainSecs] and call end on [isComplete].
 */
object HoldToEnd {
    const val HOLD_MS = 3000L
    const val STEPS = 30

    fun stepDelayMs(): Long = HOLD_MS / STEPS

    /** 0f..1f fill fraction for the given held-down time. */
    fun progress(elapsedMs: Long): Float =
        (elapsedMs / HOLD_MS.toFloat()).coerceIn(0f, 1f)

    /** True once the finger has held long enough to confirm. */
    fun isComplete(elapsedMs: Long): Boolean = elapsedMs >= HOLD_MS

    /** Whole seconds left, for the "Keep holding (N…)" label. */
    fun remainSecs(elapsedMs: Long): Int =
        ((HOLD_MS - elapsedMs + 999) / 1000).toInt().coerceAtLeast(1)
}
