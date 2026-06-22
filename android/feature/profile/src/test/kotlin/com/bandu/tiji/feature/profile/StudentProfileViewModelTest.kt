package com.bandu.tiji.feature.profile

import com.bandu.tiji.core.testing.coroutines.MainDispatcherRule
import com.bandu.tiji.core.testing.fake.FakeProfileRepository
import com.bandu.tiji.core.testing.time.TestClock
import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StudentProfileViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `future enrollment year is rejected`() = runTest {
        val repository = FakeProfileRepository()
        val clock = TestClock(
            LocalDate.of(2026, 6, 22)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli(),
        )
        val viewModel = ProfileViewModel(repository, clock)
        advanceUntilIdle()

        viewModel.onAction(ProfileAction.UpdateNickname("小明"))
        viewModel.onAction(ProfileAction.UpdateEducationStage("初中"))
        viewModel.onAction(ProfileAction.UpdateEnrollmentYear("2027"))
        viewModel.onAction(ProfileAction.SaveStudentProfile)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.studentErrorMessage)
            .isEqualTo("入学年份不能晚于当前年份")
        assertThat(repository.observeProfile().first())
            .isEqualTo(FakeProfileRepository.emptyProfile())
    }
}
