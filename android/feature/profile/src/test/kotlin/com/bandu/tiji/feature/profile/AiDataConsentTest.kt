package com.bandu.tiji.feature.profile

import com.bandu.tiji.core.testing.coroutines.MainDispatcherRule
import com.bandu.tiji.core.testing.fake.FakeProfileRepository
import com.bandu.tiji.domain.ai.AiDataConsentCoordinator
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AiDataConsentTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `first request shows disclosure and later requests continue directly`() = runTest {
        val coordinator = AiDataConsentCoordinator()
        val viewModel = ProfileViewModel(
            profileRepository = FakeProfileRepository(),
            aiDataConsent = coordinator,
        )

        viewModel.onAction(ProfileAction.RequestAiDataConsent)
        assertThat(viewModel.uiState.value.showAiDataConsent).isTrue()

        val firstEffect = async { viewModel.effects.first() }
        viewModel.onAction(ProfileAction.ConfirmAiDataConsent)
        assertThat(firstEffect.await()).isEqualTo(ProfileEffect.AiDataConsentGranted)
        assertThat(coordinator.isAccepted()).isTrue()

        val nextEffect = async { viewModel.effects.first() }
        viewModel.onAction(ProfileAction.RequestAiDataConsent)
        assertThat(nextEffect.await()).isEqualTo(ProfileEffect.AiDataConsentGranted)
        assertThat(viewModel.uiState.value.showAiDataConsent).isFalse()
    }
}
