package com.bandu.tiji.feature.library

import com.bandu.tiji.core.testing.coroutines.MainDispatcherRule
import com.bandu.tiji.core.testing.fake.FakeErrorItemRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ErrorItemSearchTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `search emits only final keyword after three hundred milliseconds`() = runTest {
        val viewModel = ErrorItemListViewModel(FakeErrorItemRepository())

        viewModel.onAction(ErrorItemListAction.UpdateKeyword("函"))
        advanceTimeBy(100)
        viewModel.onAction(ErrorItemListAction.UpdateKeyword("函数"))
        advanceTimeBy(299)
        runCurrent()

        assertThat(viewModel.debouncedQuery.value.keyword).isEmpty()
        assertThat(viewModel.uiState.value.query.keyword).isEqualTo("函数")

        advanceTimeBy(1)
        runCurrent()

        assertThat(viewModel.debouncedQuery.value.keyword).isEqualTo("函数")
    }
}
