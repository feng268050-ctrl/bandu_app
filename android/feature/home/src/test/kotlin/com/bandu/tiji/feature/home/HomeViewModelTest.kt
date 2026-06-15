package com.bandu.tiji.feature.home

import com.bandu.tiji.core.model.navigation.NavigationIntent
import com.bandu.tiji.core.model.profile.StudentProfile
import com.bandu.tiji.core.testing.coroutines.MainDispatcherRule
import com.bandu.tiji.core.testing.fake.FakeProfileRepository
import com.bandu.tiji.domain.repository.ProfileRepository
import com.google.common.truth.Truth.assertThat
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `profile nickname is trimmed and exposed after loading`() = runTest {
        val repository = FakeProfileRepository(profile("  小明  "))

        val viewModel = HomeViewModel(repository)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value).isEqualTo(
            HomeUiState(
                nickname = "小明",
                isLoading = false,
            ),
        )
    }

    @Test
    fun `blank profile nickname is represented as absent`() = runTest {
        val repository = FakeProfileRepository(profile("   "))

        val viewModel = HomeViewModel(repository)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value).isEqualTo(
            HomeUiState(
                nickname = null,
                isLoading = false,
            ),
        )
    }

    @Test
    fun `profile loading failure exposes retryable error state`() = runTest {
        val viewModel = HomeViewModel(FailingProfileRepository())
        advanceUntilIdle()

        assertThat(viewModel.uiState.value).isEqualTo(
            HomeUiState(
                nickname = null,
                isLoading = false,
                errorMessage = "无法加载学生资料",
            ),
        )
    }

    @Test
    fun `retry clears failure and reloads profile`() = runTest {
        val repository = RecoveringProfileRepository(profile("小华"))
        val viewModel = HomeViewModel(repository)
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.errorMessage).isNotNull()

        viewModel.onAction(HomeAction.RetryProfile)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value).isEqualTo(
            HomeUiState(
                nickname = "小华",
                isLoading = false,
            ),
        )
        assertThat(repository.observationCount).isEqualTo(2)
    }

    @Test
    fun `open destination actions emit matching navigation effects`() = runTest {
        val viewModel = HomeViewModel(FakeProfileRepository())
        val intents = listOf(
            NavigationIntent.OpenCapture,
            NavigationIntent.OpenLibrary,
            NavigationIntent.OpenTags,
            NavigationIntent.OpenStats,
        )

        val effects = intents.map { intent ->
            viewModel.onAction(HomeAction.OpenDestination(intent))
            viewModel.effects.first()
        }

        assertThat(effects).containsExactlyElementsIn(
            intents.map(HomeEffect::Navigate),
        ).inOrder()
    }

    private fun profile(nickname: String) =
        StudentProfile(
            nickname = nickname,
            educationStage = null,
            enrollmentYear = null,
        )
}

private class FailingProfileRepository : ProfileRepository {
    override fun observeProfile(): Flow<StudentProfile> = flow {
        throw IOException("profile unavailable")
    }

    override suspend fun updateProfile(profile: StudentProfile) = Unit

    override suspend fun clearLearningData() = Unit

    override suspend fun factoryReset() = Unit
}

private class RecoveringProfileRepository(
    private val profile: StudentProfile,
) : ProfileRepository {
    var observationCount: Int = 0
        private set

    override fun observeProfile(): Flow<StudentProfile> = flow {
        observationCount += 1
        if (observationCount == 1) {
            throw IOException("profile unavailable")
        }
        emit(profile)
    }

    override suspend fun updateProfile(profile: StudentProfile) = Unit

    override suspend fun clearLearningData() = Unit

    override suspend fun factoryReset() = Unit
}
