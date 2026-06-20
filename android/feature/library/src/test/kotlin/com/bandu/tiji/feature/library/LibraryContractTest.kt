package com.bandu.tiji.feature.library

import com.bandu.tiji.core.model.erroritem.ErrorItemQuery
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LibraryContractTest {
    @Test
    fun `contracts start in deterministic empty states`() {
        assertThat(CollectionListUiState()).isEqualTo(
            CollectionListUiState(
                collections = emptyList(),
                isLoading = true,
                errorMessage = null,
                editor = null,
                pendingDelete = null,
            ),
        )
        assertThat(ErrorItemListUiState()).isEqualTo(
            ErrorItemListUiState(
                query = ErrorItemQuery(),
                selectedIds = emptySet(),
                errorMessage = null,
            ),
        )
        assertThat(ErrorItemDetailUiState()).isEqualTo(
            ErrorItemDetailUiState(
                item = null,
                isLoading = true,
                errorMessage = null,
            ),
        )
    }
}
