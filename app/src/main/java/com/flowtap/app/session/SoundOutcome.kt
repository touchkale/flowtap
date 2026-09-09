package com.flowtap.app.session

/**
 * Requested vs effective sound state. Controllers must report failures
 * instead of claiming success — restoration evidence is preserved until
 * the restore call is confirmed successful.
 */
enum class SoundApplyResult {
    APPLIED,
    NOT_GRANTED,
    DISABLED_BY_MASTER,
    FAILED,
}

data class SoundStatus(
    val requested: String,
    val effective: String,
    val restorePending: Boolean,
)
