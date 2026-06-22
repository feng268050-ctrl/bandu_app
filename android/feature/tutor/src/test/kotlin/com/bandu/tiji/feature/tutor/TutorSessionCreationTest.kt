package com.bandu.tiji.feature.tutor

import com.bandu.tiji.core.model.id.ErrorItemId
import com.bandu.tiji.core.testing.coroutines.MainDispatcherRule
import com.bandu.tiji.core.testing.fake.FakeTutorRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class TutorSessionCreationTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `create session without error item opens unbound session`() = runTest {
        val repository = FakeTutorRepository()
        val viewModel = TutorSessionsViewModel(repository)
        val effect = async { viewModel.effects.first() }

        viewModel.onAction(TutorSessionsAction.CreateSession)
        advanceUntilIdle()

        assertThat(repository.requestedErrorItemIds).containsExactly(null)
        assertThat(effect.await()).isEqualTo(
            TutorSessionsEffect.OpenSession(
                com.bandu.tiji.core.model.id.TutorSessionId("session-1"),
            ),
        )
        assertThat(viewModel.uiState.value.isCreating).isFalse()
    }

    @Test
    fun `create session from error item preserves binding`() = runTest {
        val repository = FakeTutorRepository()
        val viewModel = TutorSessionsViewModel(repository)
        val errorItemId = ErrorItemId("error-42")
        val effect = async { viewModel.effects.first() }

        viewModel.onAction(TutorSessionsAction.CreateSessionForError(errorItemId))
        advanceUntilIdle()

        assertThat(repository.requestedErrorItemIds).containsExactly(errorItemId)
        assertThat(effect.await()).isEqualTo(
            TutorSessionsEffect.OpenSession(
                com.bandu.tiji.core.model.id.TutorSessionId("session-1"),
            ),
        )
    }

    @Test
    fun `creation failure leaves list usable`() = runTest {
        val repository = FakeTutorRepository().apply {
            failures.enqueue(IllegalStateException("write failed"))
        }
        val viewModel = TutorSessionsViewModel(repository)

        viewModel.onAction(TutorSessionsAction.CreateSession)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.isCreating).isFalse()
        assertThat(viewModel.uiState.value.errorMessage).isEqualTo("无法创建辅导会话")
    }
}
