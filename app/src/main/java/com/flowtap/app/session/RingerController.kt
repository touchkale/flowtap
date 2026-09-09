package com.flowtap.app.session

import android.content.Context
import android.media.AudioManager
import com.flowtap.app.data.prefs.SettingsDataStore
import kotlinx.coroutines.flow.first

/**
 * Silent-ringer mode: sets the ringer to silent at session start and restores
 * the PRIOR ringer mode at end — same restore discipline as [DndController].
 * Note: silent transitions that toggle DND can require notification-policy
 * access on some builds, contrary to the old "needs no permission" comment.
 */
class RingerController(
    private val context: Context,
    private val prefs: SettingsDataStore,
) {
    private val am: AudioManager =
        context.getSystemService(AudioManager::class.java)

    suspend fun silence(): SoundApplyResult {
        return try {
            val current = am.ringerMode
            if (current != AudioManager.RINGER_MODE_SILENT) {
                prefs.savePriorRingerMode(current)
                prefs.setPendingRingerRestore(true)
            }
            am.ringerMode = AudioManager.RINGER_MODE_SILENT
            SoundApplyResult.APPLIED
        } catch (e: Exception) {
            SoundApplyResult.FAILED
        }
    }

    suspend fun restore(): Boolean {
        if (!prefs.pendingRingerRestore.first()) return true
        val prior = runCatching { prefs.priorRingerMode.first() }.getOrNull()
            ?: AudioManager.RINGER_MODE_NORMAL
        val ok = runCatching { am.ringerMode = prior }.isSuccess
        if (ok) {
            prefs.setPendingRingerRestore(false)
            prefs.clearPriorRingerMode()
        }
        return ok
    }
}
