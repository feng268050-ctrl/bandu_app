package com.bandu.tiji.feature.profile

import com.bandu.tiji.core.testing.coroutines.MainDispatcherRule
import com.bandu.tiji.core.testing.fake.FakeAiConfigurationRepository
import com.bandu.tiji.core.testing.fake.FakeProfileRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PrivateHttpRiskTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `private HTTP remains disabled until explicit risk confirmation`() = runTest {
        val viewModel = ProfileViewModel(
            profileRepository = FakeProfileRepository(),
            aiConfigurationRepository = FakeAiConfigurationRepository(),
        )
        advanceUntilIdle()

        viewModel.onAction(ProfileAction.RequestPrivateHttp(true))
        assertThat(viewModel.uiState.value.showPrivateHttpRiskConfirmation).isTrue()
        assertThat(viewModel.uiState.value.aiDraft.allowPrivateCleartext).isFalse()

        viewModel.onAction(ProfileAction.DismissPrivateHttp)
        assertThat(viewModel.uiState.value.aiDraft.allowPrivateCleartext).isFalse()

        viewModel.onAction(ProfileAction.RequestPrivateHttp(true))
        viewModel.onAction(ProfileAction.ConfirmPrivateHttp)
        assertThat(viewModel.uiState.value.aiDraft.allowPrivateCleartext).isTrue()
        assertThat(viewModel.uiState.value.showPrivateHttpRiskConfirmation).isFalse()
    }
}
