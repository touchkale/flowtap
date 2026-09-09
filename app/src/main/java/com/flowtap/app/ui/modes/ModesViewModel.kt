package com.flowtap.app.ui.modes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flowtap.app.data.model.FocusMode
import com.flowtap.app.data.model.FocusTag
import com.flowtap.app.di.AppContainer
import com.flowtap.app.ui.vmFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ModesViewModel(private val repo: com.flowtap.app.data.repo.FocusRepository) : ViewModel() {

    val modes: StateFlow<List<FocusMode>> = repo.modes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tags: StateFlow<List<FocusTag>> = repo.tags
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tagsByMode: StateFlow<Map<Long, List<FocusTag>>> =
        combine(repo.modes, repo.tags) { modes, tags ->
            modes.associate { mode -> mode.id to tags.filter { it.modeId == mode.id } }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val modeNames: StateFlow<Map<Long, String>> = repo.modes
        .map { list -> list.associate { it.id to it.name } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun saveMode(mode: FocusMode) {
        viewModelScope.launch { repo.updateMode(mode) }
    }

    private val saveMutex = kotlinx.coroutines.sync.Mutex()
    private val _isSaving = kotlinx.coroutines.flow.MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving

    private val _saveError = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    val saveError: StateFlow<String?> = _saveError

    /**
     * Persists the mode (insert for a new one, update otherwise) and then
     * re-binds every tag in [attachTagIds] to it — the editor's quick "tap a
     * tag to pair it with this mode". Single-flight (M2): concurrent saves
     * are serialized and [onComplete] runs only after the committed
     * transaction, so double-taps cannot create duplicates.
     */
    fun saveModeWithTags(mode: FocusMode, attachTagIds: Set<Long>, onComplete: () -> Unit) {
        viewModelScope.launch {
            // Serialize saves; ignore re-entrant taps while one is in flight.
            if (!saveMutex.tryLock()) return@launch
            try {
                _isSaving.value = true
                _saveError.value = null
                // Protect moving the active session's ending tag (M4).
                val activeModeId = com.flowtap.app.session.SessionManager.current?.modeId
                val activeTagId = com.flowtap.app.session.SessionManager.current?.tagId
                if (activeModeId != null && mode.id == activeModeId && activeTagId != null &&
                    activeTagId in attachTagIds
                ) {
                    // Moving the ending tag mid-session is deferred: save the
                    // mode now, leave the active tag where it is.
                    repo.saveModeWithTags(mode, attachTagIds - activeTagId)
                } else {
                    repo.saveModeWithTags(mode, attachTagIds)
                }
                onComplete()
            } catch (e: Exception) {
                _saveError.value = e.message ?: "Save failed"
            } finally {
                _isSaving.value = false
                saveMutex.unlock()
            }
        }
    }

    fun clearSaveError() {
        _saveError.value = null
    }

    fun duplicateMode(mode: FocusMode) {
        viewModelScope.launch {
            repo.addMode(
                mode.copy(
                    id = 0,
                    name = "${mode.name} (copy)",
                    createdAt = System.currentTimeMillis(),
                )
            )
        }
    }

    fun deleteMode(mode: FocusMode) {
        viewModelScope.launch { repo.deleteMode(mode) }
    }

    companion object {
        fun factory(container: AppContainer) = vmFactory {
            ModesViewModel(container.focusRepository)
        }
    }
}
