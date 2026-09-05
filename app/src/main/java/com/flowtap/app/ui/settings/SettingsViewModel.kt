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

    val dailyGoalMinutes: StateFlow<Int> = settings.dailyGoalMinutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsDataStore.DEFAULT_DAILY_GOAL_MIN)

    val weeklyGoalMinutes: StateFlow<Int> = settings.weeklyGoalMinutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsDataStore.DEFAULT_WEEKLY_GOAL_MIN)

    val hardcoreMode: StateFlow<Boolean> = settings.hardcoreMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val keepScreenAwake: StateFlow<Boolean> = settings.keepScreenAwake
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val themeMode: StateFlow<ThemeMode> = settings.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.SYSTEM)

    val dndMasterEnabled: StateFlow<Boolean> = settings.dndMasterEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun setDailyGoal(minutes: Int) {
        viewModelScope.launch { settings.setDailyGoal(minutes) }
    }

    fun setWeeklyGoal(minutes: Int) {
        viewModelScope.launch { settings.setWeeklyGoal(minutes) }
    }

    fun setHardcore(enabled: Boolean) {
        viewModelScope.launch { settings.setHardcoreMode(enabled) }
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