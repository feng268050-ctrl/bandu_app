package com.bandu.tiji.feature.profile

import com.bandu.tiji.core.model.profile.StudentProfile
import com.bandu.tiji.core.testing.coroutines.MainDispatcherRule
import com.bandu.tiji.core.testing.fake.FakeProfileRepository
import com.bandu.tiji.domain.ai.AiDataConsentCoordinator
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FactoryResetTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `typed confirmation resets all data and consent`() = runTest {
        val repository = FakeProfileRepository(StudentProfile("学生", "初中", 2023))
        val consent = AiDataConsentCoordinator(initiallyAccepted = true)
        val viewModel = ProfileViewModel(
            profileRepository = repository,
            aiDataConsent = consent,
        )
        advanceUntilIdle()

        viewModel.onAction(ProfileAction.RequestFactoryReset)
        viewModel.onAction(
            ProfileAction.UpdateDataConfirmationText(
                ProfileViewModel.FACTORY_RESET_CONFIRMATION_TEXT,
            ),
        )
        viewModel.onAction(ProfileAction.ConfirmFactoryReset)
        advanceUntilIdle()

        assertThat(repository.factoryResetCalls).isEqualTo(1)
        assertThat(viewModel.uiState.value.studentDraft.nickname).isEmpty()
        assertThat(viewModel.uiState.value.isAiConfigurationActive).isFalse()
        assertThat(consent.isAccepted()).isFalse()
        assertThat(viewModel.uiState.value.dataManagement.statusMessage)
            .isEqualTo("已恢复出厂设置并生成新设备身份")
    }
}
