package com.bandu.tiji.feature.tags

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bandu.tiji.domain.repository.TagRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TagsViewModel(
    private val tagRepository: TagRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(TagsUiState())
    val uiState: StateFlow<TagsUiState> = mutableUiState.asStateFlow()

    private val mutableEffects = Channel<TagsEffect>(capacity = Channel.BUFFERED)
    val effects = mutableEffects.receiveAsFlow()

    private var observationJob: Job? = null

    init {
        observeSelectedSubject()
    }

    fun onAction(action: TagsAction) {
        when (action) {
            is TagsAction.SelectSubject -> selectSubject(action.subject)
            is TagsAction.ToggleExpanded -> toggleExpanded(action.tagId)
            TagsAction.Retry -> observeSelectedSubject()
            is TagsAction.OpenTag -> mutableEffects.trySend(
                TagsEffect.OpenFilteredErrorItems(action.tagId),
            )
            is TagsAction.OpenCreate,
            is TagsAction.OpenDelete,
            is TagsAction.OpenRename,
            is TagsAction.EditorNameChanged,
            TagsAction.ConfirmDelete,
            TagsAction.DismissDelete,
            TagsAction.DismissEditor,
            TagsAction.SubmitEditor,
            -> Unit
        }
    }

    private fun selectSubject(subject: String) {
        if (subject !in mutableUiState.value.subjects ||
            subject == mutableUiState.value.selectedSubject
        ) {
            return
        }
        mutableUiState.update {
            it.copy(
                selectedSubject = subject,
                tree = emptyList(),
                expandedTagIds = emptySet(),
            )
        }
        observeSelectedSubject()
    }

    private fun toggleExpanded(tagId: com.bandu.tiji.core.model.id.TagId) {
        mutableUiState.update { state ->
            state.copy(
                expandedTagIds = if (tagId in state.expandedTagIds) {
                    state.expandedTagIds - tagId
                } else {
                    state.expandedTagIds + tagId
                },
            )
        }
    }

    private fun observeSelectedSubject() {
        observationJob?.cancel()
        mutableUiState.update {
            it.copy(
                isLoading = true,
                loadErrorMessage = null,
            )
        }
        val subject = mutableUiState.value.selectedSubject
        observationJob = viewModelScope.launch {
            tagRepository.observeTree(subject)
                .catch {
                    mutableUiState.update { state ->
                        state.copy(
                            isLoading = false,
                            loadErrorMessage = LOAD_ERROR_MESSAGE,
                        )
                    }
                }
                .collect { tree ->
                    mutableUiState.update { state ->
                        state.copy(
                            tree = tree,
                            isLoading = false,
                            loadErrorMessage = null,
                        )
                    }
                }
        }
    }

    private companion object {
        const val LOAD_ERROR_MESSAGE = "无法加载标签"
    }
}
