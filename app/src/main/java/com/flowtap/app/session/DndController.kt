package com.flowtap.app.session

import android.app.NotificationManager
import android.content.Context
import com.flowtap.app.data.prefs.SettingsDataStore
import kotlinx.coroutines.flow.first

/**
 * Enables Do Not Disturb at session start and restores the PRIOR filter at end.
 * We never blindly turn DND off — we restore whatever the user had before,
 * so a manually-enabled DND survives a session.
 */
class DndController(
    private val context: Context,
    private val prefs: SettingsDataStore,
) {
    private val nm: NotificationManager =
        context.getSystemService(NotificationManager::class.java)

    fun isPolicyAccessGranted(): Boolean = nm.isNotificationPolicyAccessGranted

    suspend fun enableDnd() {
        if (!isPolicyAccessGranted()) return
        runCatching {
            val current = nm.currentInterruptionFilter
            // F8: never persist UNKNOWN as the value to restore later.
            val toStore = if (current == NotificationManager.INTERRUPTION_FILTER_UNKNOWN) {
                NotificationManager.INTERRUPTION_FILTER_ALL
            } else {
                current
            }
            prefs.savePriorDndFilter(toStore)
            prefs.setPendingDndRestore(true)
            nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)
        }
    }

    suspend fun restoreDnd() {
        if (!prefs.pendingDndRestore.first()) return
        // previous filter is stored by enableDnd; read it if present, else ALL
        val priorFilter = getStoredPriorFilter() ?: NotificationManager.INTERRUPTION_FILTER_ALL
        runCatching { nm.setInterruptionFilter(priorFilter) }
        prefs.setPendingDndRestore(false)
        prefs.clearPriorDndFilter()
    }

    private suspend fun getStoredPriorFilter(): Int? =
        runCatching { prefs.priorDndFilter.first() }.getOrNull()
}
