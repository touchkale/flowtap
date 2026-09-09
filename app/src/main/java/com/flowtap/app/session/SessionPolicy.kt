package com.flowtap.app.session

import com.flowtap.app.data.model.ActiveSessionSnapshot
import com.flowtap.app.data.model.EndReason
import com.flowtap.app.data.model.FocusMode
import com.flowtap.app.data.model.FocusTag

/**
 * One domain policy for session ending, used by every surface (Home,
 * notification action, both overlays, launcher shortcut, NFC taps).
 *
 * Hardcore is a global UI/enforcement lock; strict is per-mode tag ending.
 * Emergency + timeout + reboot always remain available — visual
 * simplification must never remove an emergency route.
 */
object SessionPolicy {

    /** End UI/actions are locked when the running session is strict or Hardcore is on. */
    fun isEndLocked(strictMode: Boolean, hardcore: Boolean): Boolean =
        strictMode || hardcore

    fun isEndLocked(snap: ActiveSessionSnapshot, hardcore: Boolean): Boolean =
        isEndLocked(snap.strictMode, hardcore)

    /**
     * Whether a manual end command is allowed. Emergency, timeout and reboot
     * bypass the lock by design; ordinary in-app / tap / switch ends do not.
     */
    fun canEnd(reason: EndReason, strictMode: Boolean, hardcore: Boolean): Boolean {
        if (reason == EndReason.EMERGENCY || reason == EndReason.TIMEOUT || reason == EndReason.REBOOT) {
            return true
        }
        return !isEndLocked(strictMode, hardcore)
    }

    /**
     * Strict sessions end only with the starting tag. Returns true when this
     * tap is allowed to end the running session.
     *
     * - Strict + known starting tag: only that exact tag ends it.
     * - Strict + unknown starting tag (legacy null): no tap ends it; use
     *   emergency hold or timeout.
     * - Flexible: ending on the same mode preserves the historical behavior
     *   (any tag bound to the same mode ends it).
     */
    fun strictTapEndsSession(
        current: ActiveSessionSnapshot,
        tappedMode: FocusMode,
        tappedTag: FocusTag?,
    ): Boolean {
        if (current.modeId != tappedMode.id) return false
        if (!current.strictMode) return true
        val startedTagId = current.tagId ?: return false
        return tappedTag?.id == startedTagId
    }
}
