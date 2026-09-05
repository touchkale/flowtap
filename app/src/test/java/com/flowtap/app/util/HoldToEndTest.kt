package com.flowtap.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HoldToEndTest {

    @Test
    fun progress_rampsLinearlyAndClamps() {
        assertEquals(0f, HoldToEnd.progress(0L))
        assertEquals(0.5f, HoldToEnd.progress(1500L))
        assertEquals(1f, HoldToEnd.progress(3000L))
        assertEquals(1f, HoldToEnd.progress(9999L))
        assertEquals(0f, HoldToEnd.progress(-50L))
    }

    @Test
    fun isComplete_onlyAtThreshold() {
        assertFalse(HoldToEnd.isComplete(0L))
        assertFalse(HoldToEnd.isComplete(2999L))
        assertTrue(HoldToEnd.isComplete(3000L))
        assertTrue(HoldToEnd.isComplete(5000L))
    }

    @Test
    fun remainSecs_countsDown() {
        assertEquals(3, HoldToEnd.remainSecs(0L))
        assertEquals(3, HoldToEnd.remainSecs(999L))
        assertEquals(2, HoldToEnd.remainSecs(1000L))
        assertEquals(1, HoldToEnd.remainSecs(2000L))
        assertEquals(1, HoldToEnd.remainSecs(2999L))
    }

    @Test
    fun steps_coverFullHold() {
        assertEquals(HoldToEnd.HOLD_MS, HoldToEnd.stepDelayMs() * HoldToEnd.STEPS)
    }
}
