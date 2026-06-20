package com.bandu.tiji.feature.tags

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bandu.tiji.core.common.result.AppError
import com.bandu.tiji.core.common.result.AppResult
import com.bandu.tiji.domain.tag.CreateTagInput
import com.bandu.tiji.domain.repository.TagRepository
import com.bandu.tiji.domain.usecase.tag.CreateCustomTagUseCase
import com.bandu.tiji.domain.usecase.tag.DeleteCustomTagUseCase
import com.bandu.tiji.domain.usecase.tag.RenameCustomTagUseCase
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
    private val createCustomTag: CreateCustomTagUseCase =
        CreateCustomTagUseCase(tagRepository),
    private val renameCustomTag: RenameCustomTagUseCase =
        RenameCustomTagUseCase(tagRepository),
    private val deleteCustomTag: DeleteCustomTagUseCase =
        DeleteCustomTagUseCase(tagRepository),
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(TagsUiState())
    val uiState: StateFlow<TagsUiState> = mutableUiState.asStateFlow()

    private val mutableEffects = Channel<TagsEffect>(capacity = Channel.BUFFERED)
    val effects = mutableEffects.receiveAsFlow()

    private var observationJob: Job? = null
    private var mutationJob: Job? = null

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
            is TagsAction.OpenCreate -> openCreate(action.parentId)
            is TagsAction.OpenRename -> openRename(action.node)
            is TagsAction.OpenDelete -> openDelete(action.node)
            is TagsAction.EditorNameChanged -> updateEditorName(action.name)
            TagsAction.DismissEditor -> dismissEditor()
            TagsAction.SubmitEditor -> submitEditor()
            TagsAction.ConfirmDelete -> confirmDelete()
            TagsAction.DismissDelete -> dismissDelete()
        }
    }

    private fun openCreate(parentId: com.bandu.tiji.core.model.id.TagId?) {
        mutationJob?.cancel()
        mutableUiState.update {
            it.copy(
                editor = TagEditorState(
                    mode = TagEditorMode.Create(parentId),
                ),
            )
        }
    }

    private fun updateEditorName(name: String) {
        mutableUiState.update { state ->
            state.copy(
                editor = state.editor?.copy(
                    name = name,
                    errorMessage = null,
                ),
            )
        }
    }

    private fun openRename(node: com.bandu.tiji.core.model.tag.TagNode) {
        if (node.tag.isSystem) return
        mutationJob?.cancel()
        mutableUiState.update {
            it.copy(
                editor = TagEditorState(
                    mode = TagEditorMode.Rename(
                        tagId = node.tag.id,
                        originalName = node.tag.name,
                    ),
                    name = node.tag.name,
                ),
            )
        }
    }

    private fun dismissEditor() {
        mutationJob?.cancel()
        mutationJob = null
        mutableUiState.update { it.copy(editor = null) }
    }

    private fun openDelete(node: com.bandu.tiji.core.model.tag.TagNode) {
        if (node.tag.isSystem) return
        mutationJob?.cancel()
        mutableUiState.update {
            it.copy(
                deleteConfirmation = TagDeleteConfirmation(
                    tagId = node.tag.id,
                    name = node.tag.name,
                    linkedErrorItemCount = node.linkedErrorItemCount,
                ),
            )
        }
    }

    private fun dismissDelete() {
        mutationJob?.cancel()
        mutationJob = null
        mutableUiState.update { it.copy(deleteConfirmation = null) }
    }

    private fun confirmDelete() {
        val confirmation = mutableUiState.value.deleteConfirmation ?: return
        if (confirmation.isSubmitting) return
        mutableUiState.update {
            it.copy(
                deleteConfirmation = confirmation.copy(
                    isSubmitting = true,
                    errorMessage = null,
                ),
            )
        }
        mutationJob = viewModelScope.launch {
            when (deleteCustomTag(confirmation.tagId)) {
                is AppResult.Success -> mutableUiState.update {
                    it.copy(deleteConfirmation = null)
                }
                is AppResult.Failure -> mutableUiState.update { state ->
                    state.copy(
                        deleteConfirmation = state.deleteConfirmation?.copy(
                            isSubmitting = false,
                            errorMessage = "无法删除标签",
                        ),
                    )
                }
            }
        }
    }

    private fun submitEditor() {
        val editor = mutableUiState.value.editor ?: return
        if (editor.isSubmitting) return
        mutableUiState.update {
            it.copy(editor = editor.copy(isSubmitting = true, errorMessage = null))
        }
        mutationJob = viewModelScope.launch {
            val result = when (val mode = editor.mode) {
                is TagEditorMode.Create -> createCustomTag(
                    CreateTagInput(
                        name = editor.name,
                        subject = mutableUiState.value.selectedSubject,
                        parentId = mode.parentId,
                    ),
                ).map { Unit }
                is TagEditorMode.Rename -> renameCustomTag(mode.tagId, editor.name)
            }
            when (result) {
                is AppResult.Success -> mutableUiState.update { it.copy(editor = null) }
                is AppResult.Failure -> mutableUiState.update { state ->
                    state.copy(
                        editor = state.editor?.copy(
                            isSubmitting = false,
                            errorMessage = editorErrorMessage(
                                error = result.error,
                                mode = editor.mode,
                            ),
                        ),
                    )
                }
            }
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

        fun editorErrorMessage(
            error: AppError,
            mode: TagEditorMode,
        ): String =
            when {
                error is AppError.Validation && error.code == "tag.name.blank" ->
                    "请输入标签名称"
                error.isDuplicateTagName() -> "同一位置已存在同名标签"
                mode is TagEditorMode.Create -> "无法创建标签"
                else -> "无法修改标签"
            }

        fun AppError.isDuplicateTagName(): Boolean {
            val message = (this as? AppError.Unexpected)
                ?.cause
                ?.message
                ?.lowercase()
                ?: return false
            return "already exists" in message ||
                "unique" in message ||
                "constraint" in message
        }
    }
}
