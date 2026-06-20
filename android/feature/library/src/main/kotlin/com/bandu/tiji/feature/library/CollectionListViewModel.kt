package com.bandu.tiji.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bandu.tiji.core.common.result.AppError
import com.bandu.tiji.core.common.result.AppResult
import com.bandu.tiji.core.model.navigation.NavigationIntent
import com.bandu.tiji.domain.repository.CollectionRepository
import com.bandu.tiji.domain.usecase.collection.CreateCollectionUseCase
import com.bandu.tiji.domain.usecase.collection.RenameCollectionUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class CollectionListViewModel(
    private val repository: CollectionRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(CollectionListUiState())
    val uiState: StateFlow<CollectionListUiState> = mutableUiState.asStateFlow()

    private val mutableEffects = Channel<CollectionListEffect>(Channel.BUFFERED)
    val effects = mutableEffects.receiveAsFlow()

    private var collectionJob: Job? = null
    private var editorJob: Job? = null
    private val createCollection = CreateCollectionUseCase(repository)
    private val renameCollection = RenameCollectionUseCase(repository)

    init {
        loadCollections()
    }

    fun onAction(action: CollectionListAction) {
        when (action) {
            CollectionListAction.Retry -> loadCollections()
            CollectionListAction.RequestCreate -> {
                editorJob?.cancel()
                mutableUiState.value = mutableUiState.value.copy(
                    editor = CollectionEditorUiState(
                        mode = CollectionEditorMode.Create,
                        name = "",
                    ),
                )
            }
            is CollectionListAction.RequestRename -> {
                val collection = mutableUiState.value.collections
                    .firstOrNull { it.id == action.id }
                    ?: return
                editorJob?.cancel()
                mutableUiState.value = mutableUiState.value.copy(
                    editor = CollectionEditorUiState(
                        mode = CollectionEditorMode.Rename(action.id),
                        name = collection.name,
                    ),
                )
            }
            is CollectionListAction.UpdateEditorName -> {
                mutableUiState.value = mutableUiState.value.copy(
                    editor = mutableUiState.value.editor?.copy(
                        name = action.name,
                        errorMessage = null,
                    ),
                )
            }
            CollectionListAction.SubmitEditor -> submitEditor()
            CollectionListAction.DismissEditor -> {
                editorJob?.cancel()
                mutableUiState.value = mutableUiState.value.copy(editor = null)
            }
            is CollectionListAction.OpenCollection -> {
                mutableEffects.trySend(
                    CollectionListEffect.Navigate(
                        NavigationIntent.OpenCollection(action.id.value),
                    ),
                )
            }
        }
    }

    private fun loadCollections() {
        collectionJob?.cancel()
        mutableUiState.value = CollectionListUiState()
        collectionJob = viewModelScope.launch {
            repository.observeCollections()
                .catch {
                    mutableUiState.value = CollectionListUiState(
                        isLoading = false,
                        errorMessage = COLLECTION_LOAD_ERROR,
                    )
                }
                .collect { collections ->
                    mutableUiState.value = mutableUiState.value.copy(
                        collections = collections.sortedByDescending { it.updatedAtEpochMillis },
                        isLoading = false,
                        errorMessage = null,
                    )
                }
        }
    }

    private fun submitEditor() {
        val editor = mutableUiState.value.editor ?: return
        val trimmedName = editor.name.trim()
        val duplicate = mutableUiState.value.collections.any { collection ->
            val isCurrent = (editor.mode as? CollectionEditorMode.Rename)?.id == collection.id
            !isCurrent && collection.name.equals(trimmedName, ignoreCase = true)
        }
        if (duplicate) {
            updateEditorError("题集名称已存在")
            return
        }

        editorJob?.cancel()
        mutableUiState.value = mutableUiState.value.copy(
            editor = editor.copy(isSaving = true, errorMessage = null),
        )
        editorJob = viewModelScope.launch {
            val result = when (val mode = editor.mode) {
                CollectionEditorMode.Create -> createCollection(editor.name)
                is CollectionEditorMode.Rename -> renameCollection(mode.id, editor.name)
            }
            when (result) {
                is AppResult.Success -> {
                    mutableUiState.value = mutableUiState.value.copy(editor = null)
                }
                is AppResult.Failure -> {
                    updateEditorError(result.error.toCollectionMessage())
                }
            }
        }
    }

    private fun updateEditorError(message: String) {
        mutableUiState.value = mutableUiState.value.copy(
            editor = mutableUiState.value.editor?.copy(
                errorMessage = message,
                isSaving = false,
            ),
        )
    }

    private fun AppError.toCollectionMessage(): String =
        when (this) {
            is AppError.Validation -> when (code) {
                "collection.name.blank" -> "题集名称不能为空"
                "collection.name.too_long" -> "题集名称不能超过 64 个字符"
                else -> "题集名称无效"
            }
            else -> "无法保存题集"
        }

    private companion object {
        const val COLLECTION_LOAD_ERROR = "无法加载题集"
    }
}
