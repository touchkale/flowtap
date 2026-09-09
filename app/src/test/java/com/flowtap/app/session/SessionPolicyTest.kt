package com.flowtap.app.session

import com.flowtap.app.data.model.ActiveSessionSnapshot
import com.flowtap.app.data.model.EndReason
import com.flowtap.app.data.model.FocusMode
import com.flowtap.app.data.model.FocusTag
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionPolicyTest {

    private fun snap(strict: Boolean, tagId: Long? = 7L) = ActiveSessionSnapshot(
        sessionId = 1,
        modeId = 10,
        tagId = tagId,
        modeName = "Deep Work",
        iconName = "work",
        strictMode = strict,
        startedAtEpoch = 1_000L,
    )

    private fun mode() = FocusMode(id = 10, name = "Deep Work", iconName = "work")
    private fun tag(id: Long) = FocusTag(id = id, name = "Desk", uidHex = "AA", modeId = 10)

    @Test
    fun endLocked_requiresStrictOrHardcore() {
        assertTrue(SessionPolicy.isEndLocked(strictMode = true, hardcore = false))
        assertTrue(SessionPolicy.isEndLocked(strictMode = false, hardcore = true))
        assertTrue(SessionPolicy.isEndLocked(strictMode = true, hardcore = true))
        assertFalse(SessionPolicy.isEndLocked(strictMode = false, hardcore = false))
    }

    @Test
    fun canEnd_preservesEmergencyTimeoutReboot() {
        assertTrue(SessionPolicy.canEnd(EndReason.EMERGENCY, true, true))
        assertTrue(SessionPolicy.canEnd(EndReason.TIMEOUT, true, true))
        assertTrue(SessionPolicy.canEnd(EndReason.REBOOT, true, true))
        assertFalse(SessionPolicy.canEnd(EndReason.IN_APP, true, false))
        assertFalse(SessionPolicy.canEnd(EndReason.IN_APP, false, true))
        assertFalse(SessionPolicy.canEnd(EndReason.TAP_END, true, false))
        assertTrue(SessionPolicy.canEnd(EndReason.IN_APP, false, false))
    }

    @Test
    fun strictTap_requiresStartingTag() {
        val strict = snap(strict = true, tagId = 7L)
        // Same mode + starting tag ends.
        assertTrue(SessionPolicy.strictTapEndsSession(strict, mode(), tag(7L)))
        // Same mode + different tag does not.
        assertFalse(SessionPolicy.strictTapEndsSession(strict, mode(), tag(9L)))
        // Same mode + no tag does not.
        assertFalse(SessionPolicy.strictTapEndsSession(strict, mode(), null))
        // Different mode never ends via this path.
        assertFalse(
            SessionPolicy.strictTapEndsSession(
                strict,
                mode().copy(id = 11),
                tag(7L),
            ),
        )
    }

    @Test
    fun strictTap_withoutStartingTag_neverEndsByTap() {
        val strictNoTag = snap(strict = true, tagId = null)
        assertFalse(SessionPolicy.strictTapEndsSession(strictNoTag, mode(), tag(7L)))
    }

    @Test
    fun flexibleTap_endsOnSameMode() {
        val flex = snap(strict = false)
        assertTrue(SessionPolicy.strictTapEndsSession(flex, mode(), tag(99L)))
        assertTrue(SessionPolicy.strictTapEndsSession(flex, mode(), null))
    }
}
