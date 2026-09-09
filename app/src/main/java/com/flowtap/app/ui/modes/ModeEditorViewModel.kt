package com.flowtap.app.ui.modes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flowtap.app.data.model.FocusMode
import com.flowtap.app.di.AppContainer
import com.flowtap.app.ui.vmFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Editor-scoped draft holder (M1/M2). The draft lives in the ViewModel, not
 * in `remember(mode.id)`, so rotation and activity recreation retain input.
 * Saves are single-flight and transactional; failures retain the draft.
 */
class ModeEditorViewModel(
    private val container: AppContainer,
) : ViewModel() {

    private val repo get() = container.focusRepository

    val modes: StateFlow<List<FocusMode>> = repo.modes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val tags = repo.tags
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val tagsByMode = combine(repo.modes, repo.tags) { modes, tags ->
        modes.associate { mode -> mode.id to tags.filter { it.modeId == mode.id } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())
    val modeNames: StateFlow<Map<Long, String>> = repo.modes
        .map { list: List<FocusMode> -> list.associate { mode: FocusMode -> mode.id to mode.name } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private val saveMutex = Mutex()
    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving

    private val _saveError = MutableStateFlow<String?>(null)
    val saveError: StateFlow<String?> = _saveError

    private var editorState: ModeEditorState? = null
    private var editorId: Long? = null

    /** Returns the draft, creating it once per mode id (survives rotation). */
    fun draftFor(mode: FocusMode): ModeEditorState {
        val current = editorState
        if (current != null && editorId == mode.id) return current
        val fresh = ModeEditorState(mode)
        editorState = fresh
        editorId = mode.id
        attachIds.value = emptySet()
        return fresh
    }

    private val attachIds = MutableStateFlow<Set<Long>>(emptySet())
    val attachTagIds: StateFlow<Set<Long>> = attachIds

    fun toggleAttach(tagId: Long) {
        attachIds.value = if (tagId in attachIds.value) {
            attachIds.value - tagId
        } else {
            attachIds.value + tagId
        }
    }

    fun save(
        updated: FocusMode,
        attachTagIds: Set<Long>,
        onComplete: () -> Unit,
    ) {
        viewModelScope.launch {
            saveMutex.withLock {
                _isSaving.value = true
                _saveError.value = null
                try {
                    val active = com.flowtap.app.session.SessionManager.current
                    val ids = if (active != null && updated.id == active.modeId &&
                        active.tagId != null && active.tagId in attachTagIds
                    ) {
                        // M4: don't move the active session's ending tag mid-run.
                        attachTagIds - active.tagId
                    } else {
                        attachTagIds
                    }
                    repo.saveModeWithTags(updated, ids)
                    onComplete()
                } catch (e: Exception) {
                    _saveError.value = e.message ?: "Save failed"
                } finally {
                    _isSaving.value = false
                }
            }
        }
    }

    fun clearSaveError() {
        _saveError.value = null
    }

    companion object {
        fun factory(container: AppContainer) = vmFactory {
            ModeEditorViewModel(container)
        }
    }
}
