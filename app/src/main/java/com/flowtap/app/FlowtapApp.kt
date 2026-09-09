package com.flowtap.app

import android.app.Application
import com.flowtap.app.di.AppContainer
import com.flowtap.app.session.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
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
        // F1/D3: seed the default "Deep Work" mode once ever — never after
        // intentional deletion.
        appScope.launch {
            val prefs = container.settingsDataStore
            if (!prefs.seededOnce.first()) {
                if (container.focusRepository.modeCount() == 0) {
                    container.focusRepository.ensureSeeded()
                }
                prefs.setSeededOnce()
            }
        }
        // Sync synchronous engine caches for services/overlays (S2/S4/U1).
        appScope.launch {
            kotlinx.coroutines.flow.combine(
                container.settingsDataStore.blockingMethod,
                container.settingsDataStore.hardcoreMode,
                container.settingsDataStore.themeMode,
            ) { method, hardcore, theme ->
                com.flowtap.app.session.EngineState.blockingMethod = method
                com.flowtap.app.session.EngineState.hardcore = hardcore
                val sysDark = resources.configuration.uiMode and
                    android.content.res.Configuration.UI_MODE_NIGHT_MASK ==
                    android.content.res.Configuration.UI_MODE_NIGHT_YES
                com.flowtap.app.session.EngineState.darkTheme = when (theme) {
                    com.flowtap.app.data.prefs.ThemeMode.LIGHT -> false
                    com.flowtap.app.data.prefs.ThemeMode.DARK -> true
                    com.flowtap.app.data.prefs.ThemeMode.SYSTEM -> sysDark
                }
            }.collect {}
        }
    }
}
