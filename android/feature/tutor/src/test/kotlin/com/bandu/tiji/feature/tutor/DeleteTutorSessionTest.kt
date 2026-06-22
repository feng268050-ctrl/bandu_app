package com.bandu.tiji.feature.tutor

import com.bandu.tiji.core.testing.coroutines.MainDispatcherRule
import com.bandu.tiji.core.testing.fake.FakeTutorRepository
import com.bandu.tiji.core.testing.fixture.sessionFixture
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DeleteTutorSessionTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `confirmed deletion removes only selected session`() = runTest {
        val first = sessionFixture()
        val second = sessionFixture { id = com.bandu.tiji.core.model.id.TutorSessionId("two") }
        val repository = FakeTutorRepository(listOf(first, second))
        val viewModel = TutorSessionsViewModel(repository)
        advanceUntilIdle()

        viewModel.onAction(TutorSessionsAction.RequestDelete(first.id))
        viewModel.onAction(TutorSessionsAction.ConfirmDelete)
        advanceUntilIdle()

        assertThat(repository.deletedSessionIds).containsExactly(first.id)
        assertThat(viewModel.uiState.value.sessions.map { it.id }).containsExactly(second.id)
        assertThat(viewModel.uiState.value.pendingDelete).isNull()
    }

    @Test
    fun `delete failure preserves confirmation for retry`() = runTest {
        val session = sessionFixture()
        val repository = FakeTutorRepository(listOf(session)).apply {
            failures.enqueue(IllegalStateException("disk"))
        }
        val viewModel = TutorSessionsViewModel(repository)
        advanceUntilIdle()

        viewModel.onAction(TutorSessionsAction.RequestDelete(session.id))
        viewModel.onAction(TutorSessionsAction.ConfirmDelete)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.pendingDelete).isEqualTo(session.id)
        assertThat(viewModel.uiState.value.deleteErrorMessage)
            .isEqualTo("无法删除辅导会话")
        assertThat(viewModel.uiState.value.isDeleting).isFalse()
    }
}
