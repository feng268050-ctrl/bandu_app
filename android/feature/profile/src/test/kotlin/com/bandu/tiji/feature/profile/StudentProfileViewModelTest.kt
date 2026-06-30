package com.bandu.tiji.feature.profile

import com.bandu.tiji.core.testing.coroutines.MainDispatcherRule
import com.bandu.tiji.core.testing.fake.FakeProfileRepository
import com.bandu.tiji.core.testing.time.TestClock
import com.bandu.tiji.core.model.profile.StudentProfile
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
    fun `successful save shows confirmation message`() = runTest {
        val repository = FakeProfileRepository()
        val viewModel = ProfileViewModel(
            profileRepository = repository,
        )
        advanceUntilIdle()

        viewModel.onAction(ProfileAction.UpdateNickname("小明"))
        viewModel.onAction(ProfileAction.UpdateEducationStage("初中"))
        viewModel.onAction(ProfileAction.UpdateEnrollmentYear("2024"))
        viewModel.onAction(ProfileAction.SaveStudentProfile)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.studentErrorMessage).isNull()
        assertThat(viewModel.uiState.value.studentStatusMessage).isEqualTo("学生资料已保存")
        assertThat(repository.observeProfile().first()).isEqualTo(
            StudentProfile(
                nickname = "小明",
                educationStage = "初中",
                enrollmentYear = 2024,
            ),
        )
    }

    @Test
    fun `student summary calculates grade from enrollment year`() = runTest {
        val repository = FakeProfileRepository(
            initialProfile = StudentProfile(
                nickname = "小明",
                educationStage = "初中",
                enrollmentYear = 2024,
            ),
        )
        val clock = TestClock(
            LocalDate.of(2026, 6, 30)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli(),
        )
        val viewModel = ProfileViewModel(
            profileRepository = repository,
            clock = clock,
        )
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.studentSummary).isEqualTo(
            StudentProfileSummary(
                nickname = "小明",
                educationStage = "初中",
                grade = 2,
            ),
        )
    }

    @Test
    fun `future enrollment year is rejected`() = runTest {
        val repository = FakeProfileRepository()
        val clock = TestClock(
            LocalDate.of(2026, 6, 22)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli(),
        )
        val viewModel = ProfileViewModel(
            profileRepository = repository,
            clock = clock,
        )
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
