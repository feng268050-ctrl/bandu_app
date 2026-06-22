package com.bandu.tiji.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bandu.tiji.core.model.id.ErrorItemId
import com.bandu.tiji.core.model.erroritem.ErrorItemDraft
import com.bandu.tiji.core.model.erroritem.ErrorItemPatch
import com.bandu.tiji.core.model.tag.TagNode
import com.bandu.tiji.core.common.result.AppResult
import com.bandu.tiji.domain.repository.CollectionRepository
import com.bandu.tiji.domain.repository.ErrorItemRepository
import com.bandu.tiji.domain.repository.TagRepository
import com.bandu.tiji.domain.usecase.erroritem.UpdateErrorItemUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class ErrorItemDetailViewModel(
    private val repository: ErrorItemRepository,
    private val errorItemId: ErrorItemId,
    private val collectionRepository: CollectionRepository? = null,
    private val tagRepository: TagRepository? = null,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(ErrorItemDetailUiState())
    val uiState: StateFlow<ErrorItemDetailUiState> = mutableUiState.asStateFlow()

    private val mutableEffects = Channel<ErrorItemDetailEffect>(Channel.BUFFERED)
    val effects = mutableEffects.receiveAsFlow()
    private var observationJob: Job? = null
    private var saveJob: Job? = null
    private val updateErrorItem = UpdateErrorItemUseCase(repository)

    init {
        observeItem()
        observeEditorOptions()
    }

    fun onAction(action: ErrorItemDetailAction) {
        when (action) {
            ErrorItemDetailAction.NavigateBack ->
                mutableEffects.trySend(ErrorItemDetailEffect.NavigateBack)
            ErrorItemDetailAction.Retry -> observeItem()
            ErrorItemDetailAction.OpenEditor -> openEditor()
            ErrorItemDetailAction.DismissEditor -> {
                saveJob?.cancel()
                mutableUiState.value = mutableUiState.value.copy(
                    editor = null,
                    editorErrorMessage = null,
                    isSaving = false,
                )
            }
            is ErrorItemDetailAction.UpdateEditor -> {
                mutableUiState.value = mutableUiState.value.copy(
                    editor = action.draft,
                    editorErrorMessage = null,
                )
            }
            ErrorItemDetailAction.RequestImageReplacement ->
                mutableEffects.trySend(ErrorItemDetailEffect.SelectReplacementImage)
            is ErrorItemDetailAction.ReplaceImage -> {
                mutableUiState.value = mutableUiState.value.copy(
                    editor = mutableUiState.value.editor?.copy(image = action.image),
                )
            }
            ErrorItemDetailAction.SaveEditor -> saveEditor()
        }
    }

    private fun observeItem() {
        observationJob?.cancel()
        mutableUiState.value = mutableUiState.value.copy(
            item = null,
            isLoading = true,
            errorMessage = null,
            editor = null,
            editorErrorMessage = null,
            isSaving = false,
        )
        observationJob = viewModelScope.launch {
            repository.observe(errorItemId)
                .catch {
                    mutableUiState.value = mutableUiState.value.copy(
                        item = null,
                        isLoading = false,
                        errorMessage = "无法加载错题详情",
                    )
                }
                .collect { item ->
                    mutableUiState.value = mutableUiState.value.copy(
                        item = item,
                        isLoading = false,
                        errorMessage = if (item == null) "错题不存在" else null,
                    )
                }
        }
    }

    private fun openEditor() {
        val item = mutableUiState.value.item ?: return
        mutableUiState.value = mutableUiState.value.copy(
            editor = ErrorItemEditDraft(
                image = item.image,
                questionText = item.questionText,
                answerText = item.answerText,
                analysis = item.analysis,
                wrongAnswerText = item.wrongAnswerText,
                mistakeStatus = item.mistakeStatus,
                mistakeAnalysis = item.mistakeAnalysis,
                collectionId = item.collectionId,
                subject = item.subject,
                tagIds = item.tags.map { it.id }.toSet(),
                gradeSemester = item.gradeSemester.orEmpty(),
                paperLevel = item.paperLevel,
                notes = item.notes,
            ),
            editorErrorMessage = null,
        )
    }

    private fun saveEditor() {
        val draft = mutableUiState.value.editor ?: return
        if (mutableUiState.value.isSaving) return
        val validationError = when {
            draft.questionText.isBlank() -> "题目不能为空"
            draft.answerText.isBlank() -> "答案不能为空"
            draft.analysis.isBlank() -> "解析不能为空"
            draft.subject.isBlank() -> "学科不能为空"
            draft.tagIds.size > ErrorItemDraft.MAX_TAG_COUNT ->
                "每道错题最多选择 ${ErrorItemDraft.MAX_TAG_COUNT} 个标签"
            else -> null
        }
        if (validationError != null) {
            mutableUiState.value = mutableUiState.value.copy(
                editorErrorMessage = validationError,
            )
            return
        }
        mutableUiState.value = mutableUiState.value.copy(
            isSaving = true,
            editorErrorMessage = null,
        )
        saveJob = viewModelScope.launch {
            val patch = runCatching {
                ErrorItemPatch(
                    collectionId = draft.collectionId,
                    image = draft.image,
                    questionText = draft.questionText.trim(),
                    answerText = draft.answerText.trim(),
                    analysis = draft.analysis.trim(),
                    wrongAnswerText = draft.wrongAnswerText.trim(),
                    mistakeStatus = draft.mistakeStatus,
                    mistakeAnalysis = draft.mistakeAnalysis.trim(),
                    subject = draft.subject.trim(),
                    tagIds = draft.tagIds.toList(),
                    gradeSemester = draft.gradeSemester.trim().ifEmpty { null },
                    paperLevel = draft.paperLevel,
                    notes = draft.notes.trim(),
                )
            }.getOrElse {
                mutableUiState.value = mutableUiState.value.copy(
                    isSaving = false,
                    editorErrorMessage = "错题内容无效",
                )
                return@launch
            }
            when (updateErrorItem(errorItemId, patch)) {
                is AppResult.Success -> mutableUiState.value = mutableUiState.value.copy(
                    editor = null,
                    editorErrorMessage = null,
                    isSaving = false,
                )
                is AppResult.Failure -> mutableUiState.value = mutableUiState.value.copy(
                    isSaving = false,
                    editorErrorMessage = "无法保存错题",
                )
            }
        }
    }

    private fun observeEditorOptions() {
        collectionRepository?.let { source ->
            viewModelScope.launch {
                source.observeCollections()
                    .catch { emit(emptyList()) }
                    .collect { collections ->
                        mutableUiState.value = mutableUiState.value.copy(
                            availableCollections = collections,
                        )
                    }
            }
        }
        tagRepository?.let { source ->
            viewModelScope.launch {
                source.observeTree(null)
                    .catch { emit(emptyList()) }
                    .collect { tree ->
                        mutableUiState.value = mutableUiState.value.copy(
                            availableTags = tree.flattenTags(),
                        )
                    }
            }
        }
    }

    private fun List<TagNode>.flattenTags(): List<com.bandu.tiji.core.model.tag.TagSummary> =
        flatMap { node -> listOf(node.tag) + node.children.flattenTags() }
}
