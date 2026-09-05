package com.flowtap.app

import android.app.Application
import com.flowtap.app.di.AppContainer
import com.flowtap.app.session.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class FlowtapApp : Application() {

    lateinit var container: AppContainer
        private set

    /** Application-scoped coroutine scope for fire-and-forget work (e.g. NFC routing). */
    val appScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        SessionManager.init(container)
        // F1: guarantee the default "Deep Work" mode exists on first launch.
        appScope.launch { container.focusRepository.ensureSeeded() }
    }
}
