package com.bandu.tiji.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bandu.tiji.core.model.navigation.NavigationIntent
import com.bandu.tiji.domain.repository.CollectionRepository
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

    init {
        loadCollections()
    }

    fun onAction(action: CollectionListAction) {
        when (action) {
            CollectionListAction.Retry -> loadCollections()
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
                    mutableUiState.value = CollectionListUiState(
                        collections = collections.sortedByDescending { it.updatedAtEpochMillis },
                        isLoading = false,
                    )
                }
        }
    }

    private companion object {
        const val COLLECTION_LOAD_ERROR = "无法加载题集"
    }
}
