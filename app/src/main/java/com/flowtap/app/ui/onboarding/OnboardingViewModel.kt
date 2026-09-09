package com.flowtap.app.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flowtap.app.data.model.FocusMode
import com.flowtap.app.data.repo.FocusRepository
import com.flowtap.app.di.AppContainer
import com.flowtap.app.ui.vmFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class OnboardingViewModel(
    private val repo: FocusRepository,
    private val settings: com.flowtap.app.data.prefs.SettingsDataStore,
) : ViewModel() {

    private val _step = MutableStateFlow(1)
    val step: StateFlow<Int> = _step

    private val _selectedBlocked = MutableStateFlow<Set<String>>(emptySet())
    val selectedBlocked: StateFlow<Set<String>> = _selectedBlocked

    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving

    val hardcoreMode = settings.hardcoreMode
    val blockingMethod = settings.blockingMethod

    fun setHardcore(enabled: Boolean) {
        viewModelScope.launch { settings.setHardcoreMode(enabled) }
    }

    fun setBlockingMethod(method: com.flowtap.app.data.prefs.BlockingMethod) {
        viewModelScope.launch { settings.setBlockingMethod(method) }
    }

    fun next() {
        if (_step.value < 3) _step.value += 1
    }

    fun back() {
        if (_step.value > 1) _step.value -= 1
    }

    fun toggleBlocked(pkg: String) {
        _selectedBlocked.value = _selectedBlocked.value
            .let { if (pkg in it) it - pkg else it + pkg }
    }

    fun setSelectedBlocked(packages: Set<String>) {
        _selectedBlocked.value = packages
    }

    private val _draftModeId = MutableStateFlow<Long?>(null)
    val draftModeId: StateFlow<Long?> = _draftModeId

    /** Finds a mode by name (case-insensitive) or creates it; tracks it explicitly (O1). */
    suspend fun ensureMode(name: String, iconName: String): Long {
        _draftModeId.value?.let { id ->
            repo.modes.first().firstOrNull { it.id == id }?.let { return id }
        }
        val existing = repo.modes.first().firstOrNull { it.name.equals(name, ignoreCase = true) }
        val id = existing?.id ?: repo.addMode(FocusMode(name = name, iconName = iconName))
        _draftModeId.value = id
        return id
    }

    /**
     * Persists blocked-app selection onto the explicit onboarding target mode
     * (O1). An empty selection is honored — it clears the seeded defaults
     * instead of silently preserving them.
     */
    fun finish(onDone: () -> Unit) {
        if (_saving.value) return
        _saving.value = true
        viewModelScope.launch {
            try {
                val modes = repo.modes.first()
                val target = _draftModeId.value?.let { id -> modes.firstOrNull { it.id == id } }
                    ?: modes.firstOrNull()
                if (target != null) {
                    repo.updateMode(target.copy(blockedApps = _selectedBlocked.value.toList()))
                }
            } finally {
                _saving.value = false
                onDone()
            }
        }
    }

    companion object {
        fun factory(container: AppContainer) = vmFactory {
            OnboardingViewModel(container.focusRepository, container.settingsDataStore)
        }
    }
}