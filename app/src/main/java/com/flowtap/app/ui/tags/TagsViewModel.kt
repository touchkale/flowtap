package com.flowtap.app.ui.tags

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flowtap.app.data.model.FocusMode
import com.flowtap.app.data.model.FocusTag
import com.flowtap.app.data.repo.FocusRepository
import com.flowtap.app.di.AppContainer
import com.flowtap.app.ui.vmFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TagsViewModel(private val repo: FocusRepository) : ViewModel() {

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

    fun createMode(mode: FocusMode) {
        viewModelScope.launch { repo.addMode(mode) }
    }

    fun deleteMode(mode: FocusMode) {
        viewModelScope.launch { repo.deleteMode(mode) }
    }

    fun deleteTag(tag: FocusTag) {
        viewModelScope.launch { repo.deleteTag(tag) }
    }

    fun saveTag(tag: FocusTag) {
        viewModelScope.launch { repo.updateTag(tag) }
    }

    companion object {
        fun factory(container: AppContainer) = vmFactory {
            TagsViewModel(container.focusRepository)
        }
    }
}