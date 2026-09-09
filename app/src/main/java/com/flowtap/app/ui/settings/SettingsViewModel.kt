package com.flowtap.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flowtap.app.data.prefs.SettingsDataStore
import com.flowtap.app.data.prefs.ThemeMode
import com.flowtap.app.di.AppContainer
import com.flowtap.app.session.SessionManager
import com.flowtap.app.ui.vmFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val settings: SettingsDataStore) : ViewModel() {

    val hardcoreMode: StateFlow<Boolean> = settings.hardcoreMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val blockingMethod: StateFlow<com.flowtap.app.data.prefs.BlockingMethod> = settings.blockingMethod
        .stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000),
            com.flowtap.app.data.prefs.BlockingMethod.FULL_POWER,
        )

    val keepScreenAwake: StateFlow<Boolean> = settings.keepScreenAwake
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val themeMode: StateFlow<ThemeMode> = settings.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.SYSTEM)

    val dndMasterEnabled: StateFlow<Boolean> = settings.dndMasterEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun setHardcore(enabled: Boolean) {
        com.flowtap.app.session.EngineState.hardcore = enabled
        viewModelScope.launch { settings.setHardcoreMode(enabled) }
    }

    fun setBlockingMethod(method: com.flowtap.app.data.prefs.BlockingMethod) {
        com.flowtap.app.session.EngineState.blockingMethod = method
        viewModelScope.launch { settings.setBlockingMethod(method) }
    }

    fun setKeepScreenAwake(enabled: Boolean) {
        viewModelScope.launch { settings.setKeepScreenAwake(enabled) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settings.setThemeMode(mode) }
    }

    fun setDndMaster(enabled: Boolean) {
        viewModelScope.launch {
            settings.setDndMasterEnabled(enabled)
            SessionManager.applyDndMaster()
        }
    }

    companion object {
        fun factory(container: AppContainer) = vmFactory {
            SettingsViewModel(container.settingsDataStore)
        }
    }
}