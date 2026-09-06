package com.flowtap.app.session

import android.content.Context
import android.media.AudioManager
import com.flowtap.app.data.prefs.SettingsDataStore
import kotlinx.coroutines.flow.first

/**
 * Silent-ringer mode: sets the ringer to silent at session start and restores
 * the PRIOR ringer mode at end — same restore discipline as [DndController].
 * Needs no system permission beyond MODIFY_AUDIO_SETTINGS (normal).
 */
class RingerController(
    private val context: Context,
    private val prefs: SettingsDataStore,
) {
    private val am: AudioManager =
        context.getSystemService(AudioManager::class.java)

    suspend fun silence() {
        runCatching {
            val current = am.ringerMode
            if (current != AudioManager.RINGER_MODE_SILENT) {
                prefs.savePriorRingerMode(current)
                prefs.setPendingRingerRestore(true)
            }
            am.ringerMode = AudioManager.RINGER_MODE_SILENT
        }
    }

    suspend fun restore() {
        if (!prefs.pendingRingerRestore.first()) return
        val prior = runCatching { prefs.priorRingerMode.first() }.getOrNull()
            ?: AudioManager.RINGER_MODE_NORMAL
        runCatching { am.ringerMode = prior }
        prefs.setPendingRingerRestore(false)
        prefs.clearPriorRingerMode()
    }
}
