package com.bandu.tiji.feature.devices

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.bandu.tiji.core.designsystem.theme.BanduTijiTheme
import com.bandu.tiji.core.testing.coroutines.MainDispatcherRule
import com.bandu.tiji.core.testing.fake.FakeDeviceTransferRepository
import com.bandu.tiji.core.testing.time.TestClock
import com.bandu.tiji.domain.transfer.PairingCode
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
class ReceiveModeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `receive mode shows six digit code and expires after five minutes`() = runTest {
        val clock = TestClock(1_000L)
        val repository = FakeDeviceTransferRepository().apply {
            pairingCode = PairingCode("654321", 301_000L)
        }
        val viewModel = DevicesViewModel(repository, "本机", "fingerprint", clock)
        advanceUntilIdle()

        viewModel.onAction(DevicesAction.StartReceiveMode)
        runCurrent()
        assertThat(viewModel.uiState.value.receiveMode?.code).isEqualTo("654321")
        assertThat(viewModel.uiState.value.receiveMode?.secondsRemaining).isEqualTo(300)

        clock.advanceBy(300_000L)
        advanceTimeBy(1_000L)
        runCurrent()
        assertThat(viewModel.uiState.value.receiveMode?.isExpired).isTrue()
    }

    @Test
    fun `leaving receive mode stops discovery and clears code`() = runTest {
        val repository = FakeDeviceTransferRepository()
        val viewModel = DevicesViewModel(repository, "本机", "fingerprint")
        advanceUntilIdle()
        viewModel.onAction(DevicesAction.StartReceiveMode)
        runCurrent()

        viewModel.onAction(DevicesAction.StopReceiveMode)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.receiveMode).isNull()
        assertThat(repository.stopDiscoveryCalls).isEqualTo(1)
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ReceiveModeScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `expired code is explicit`() {
        val actions = mutableListOf<DevicesAction>()
        composeRule.setContent {
            BanduTijiTheme {
                DevicesScreen(
                    uiState = DevicesUiState(
                        localDeviceName = "本机",
                        localFingerprint = "fingerprint",
                        receiveMode = ReceiveModeUiState(
                            code = "123456",
                            expiresAtEpochMillis = 0L,
                            secondsRemaining = 0,
                            isCreating = false,
                        ),
                        isLoading = false,
                    ),
                    onAction = actions::add,
                )
            }
        }

        composeRule.onNodeWithText("配对码：123456").assertIsDisplayed()
        composeRule.onNodeWithText("配对码已过期").assertIsDisplayed()
        composeRule.onNodeWithText("取消").assertIsDisplayed()
        composeRule.onNodeWithText("取消").performClick()

        composeRule.runOnIdle {
            assertThat(actions).containsExactly(DevicesAction.StopReceiveMode)
        }
    }
}
