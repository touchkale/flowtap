package com.flowtap.app.session

import com.flowtap.app.data.prefs.BlockingMethod

/**
 * Synchronous caches for synchronous call sites (accessibility service,
 * overlay controller). Updated from coroutines at startup and on every
 * settings change — the service must never block on DataStore.
 */
object EngineState {
    @Volatile
    var blockingMethod: BlockingMethod = BlockingMethod.FULL_POWER

    @Volatile
    var hardcore: Boolean = false
}
