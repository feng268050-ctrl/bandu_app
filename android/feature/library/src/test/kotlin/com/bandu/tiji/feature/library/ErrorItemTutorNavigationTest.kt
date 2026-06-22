package com.bandu.tiji.feature.library

import com.bandu.tiji.core.model.navigation.NavigationIntent
import com.bandu.tiji.core.testing.coroutines.MainDispatcherRule
import com.bandu.tiji.core.testing.fake.FakeErrorItemRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ErrorItemTutorNavigationTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `open tutor emits intent bound to current error item`() = runTest {
        val item = detailItem()
        val viewModel = ErrorItemDetailViewModel(
            repository = FakeErrorItemRepository(listOf(item)),
            errorItemId = item.id,
        )
        advanceUntilIdle()

        val effect = async { viewModel.effects.first() }
        viewModel.onAction(ErrorItemDetailAction.OpenTutor)

        assertThat(effect.await()).isEqualTo(
            ErrorItemDetailEffect.Navigate(
                NavigationIntent.OpenTutor(item.id.value),
            ),
        )
    }
}
