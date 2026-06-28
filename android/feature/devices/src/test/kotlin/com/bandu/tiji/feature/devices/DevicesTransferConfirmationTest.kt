package com.bandu.tiji.feature.devices

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import com.bandu.tiji.core.designsystem.theme.BanduTijiTheme
import com.bandu.tiji.core.testing.coroutines.MainDispatcherRule
import com.bandu.tiji.core.testing.fake.FakeDeviceTransferRepository
import com.bandu.tiji.domain.transfer.TransferOfferSummary
import com.bandu.tiji.domain.transfer.TransferState
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
class DevicesTransferConfirmationViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `accept and reject incoming transfer use current offer session`() = runTest {
        val offer = offer()
        val repository = FakeDeviceTransferRepository(
            initialTransferState = TransferState.AwaitingOfferConfirmation(offer),
        )
        val viewModel = DevicesViewModel(repository, "本机", "LOCAL")
        advanceUntilIdle()

        viewModel.onAction(DevicesAction.AcceptIncomingTransfer)
        advanceUntilIdle()
        viewModel.onAction(DevicesAction.RejectIncomingTransfer)
        advanceUntilIdle()

        assertThat(repository.acceptedSessionIds).containsExactly("session-1")
        assertThat(repository.rejectedSessionIds).containsExactly("session-1")
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h800dp")
class DevicesTransferConfirmationScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `incoming transfer confirmation explains replacement and api key clearing`() {
        val actions = mutableListOf<DevicesAction>()
        composeRule.setContent {
            BanduTijiTheme {
                DevicesScreen(
                    uiState = DevicesUiState(
                        localDeviceName = "本机",
                        localFingerprint = "LOCAL",
                        transferState = TransferState.AwaitingOfferConfirmation(offer()),
                        isLoading = false,
                    ),
                    onAction = actions::add,
                )
            }
        }

        composeRule.onNodeWithTag("devices-list").performScrollToIndex(4)
        composeRule.onNodeWithTag(INCOMING_TRANSFER_CONFIRMATION_TAG)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("接收后，本机现有学习数据会被完整替换。")
            .assertIsDisplayed()
        composeRule.onNodeWithText("迁移成功后，本机 API Key 会被清除，需要重新配置。")
            .assertIsDisplayed()
        composeRule.onNodeWithTag(INCOMING_ACCEPT_TAG).performClick()

        composeRule.runOnIdle {
            assertThat(actions).contains(DevicesAction.AcceptIncomingTransfer)
        }
    }
}

private fun offer() =
    TransferOfferSummary(
        sessionId = "session-1",
        sourceDeviceName = "旧手机",
        totalBytes = 2_097_152L,
        totalFiles = 12,
        totalRecords = 345,
    )
