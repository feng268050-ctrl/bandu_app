package com.bandu.tiji.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.bandu.tiji.core.model.erroritem.ErrorItemQuery
import com.bandu.tiji.core.model.erroritem.ErrorItemSummary
import com.bandu.tiji.core.model.navigation.NavigationIntent
import com.bandu.tiji.domain.repository.ErrorItemRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow

@OptIn(ExperimentalCoroutinesApi::class)
class ErrorItemListViewModel(
    private val repository: ErrorItemRepository,
    initialQuery: ErrorItemQuery = ErrorItemQuery(),
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(ErrorItemListUiState(query = initialQuery))
    val uiState: StateFlow<ErrorItemListUiState> = mutableUiState.asStateFlow()

    private val query = MutableStateFlow(initialQuery)
    val pagingData: Flow<PagingData<ErrorItemSummary>> = query
        .flatMapLatest { currentQuery ->
            Pager(PagingConfig(pageSize = PAGE_SIZE, prefetchDistance = PREFETCH_DISTANCE)) {
                repository.page(currentQuery)
            }.flow
        }
        .cachedIn(viewModelScope)

    private val mutableEffects = Channel<ErrorItemListEffect>(Channel.BUFFERED)
    val effects = mutableEffects.receiveAsFlow()

    fun onAction(action: ErrorItemListAction) {
        when (action) {
            is ErrorItemListAction.OpenErrorItem -> {
                mutableEffects.trySend(
                    ErrorItemListEffect.Navigate(
                        NavigationIntent.OpenErrorItem(action.id.value),
                    ),
                )
            }
        }
    }

    companion object {
        const val PAGE_SIZE = 30
        const val PREFETCH_DISTANCE = 10
    }
}
