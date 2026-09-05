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

    /**
     * Persists the mode (insert for a new one, update otherwise) and then
     * re-binds every tag in [attachTagIds] to it — the editor's quick "tap a
     * tag to pair it with this mode". [onComplete] runs afterwards so the
     * caller can navigate back knowing both writes landed.
     */
    fun saveModeWithTags(mode: FocusMode, attachTagIds: Set<Long>, onComplete: () -> Unit) {
        viewModelScope.launch {
            val id = if (mode.id == 0L) {
                repo.addMode(mode)
            } else {
                repo.updateMode(mode)
                mode.id
            }
            if (attachTagIds.isNotEmpty()) {
                repo.tags.first().filter { it.id in attachTagIds }.forEach { tag ->
                    if (tag.modeId != id) repo.updateTag(tag.copy(modeId = id))
                }
            }
            onComplete()
        }
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
