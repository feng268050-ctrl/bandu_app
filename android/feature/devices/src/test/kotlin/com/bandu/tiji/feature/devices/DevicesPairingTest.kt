package com.bandu.tiji.feature.devices

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import com.bandu.tiji.core.designsystem.theme.BanduTijiTheme
import com.bandu.tiji.core.testing.coroutines.MainDispatcherRule
import com.bandu.tiji.core.testing.fake.FakeDeviceTransferRepository
import com.bandu.tiji.domain.transfer.DiscoveryMode
import com.bandu.tiji.domain.transfer.NearbyDevice
import com.bandu.tiji.domain.transfer.PairingResult
import com.bandu.tiji.domain.transfer.TransferFailureCode
import com.bandu.tiji.domain.transfer.TrustedDevice
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
class DevicesPairingViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `pairing submits selected device and six digit code`() = runTest {
        val device = NearbyDevice("nearby-1", "新手机", DiscoveryMode.PAIR)
        val repository = FakeDeviceTransferRepository(initialNearbyDevices = listOf(device))
        val viewModel = DevicesViewModel(repository, "本机", "fingerprint")
        advanceUntilIdle()

        viewModel.onAction(DevicesAction.SelectNearbyDevice(device))
        viewModel.onAction(DevicesAction.UpdatePairingCode("12a345678"))
        viewModel.onAction(DevicesAction.SubmitPairingCode)
        advanceUntilIdle()

        assertThat(repository.pairRequests.single().device).isEqualTo(device)
        assertThat(repository.pairRequests.single().code).isEqualTo("123456")
        assertThat(viewModel.uiState.value.pairingDialog).isNull()
        assertThat(viewModel.uiState.value.pairingConfirmation?.deviceName).isEqualTo("新手机")
    }

    @Test
    fun `pairing errors are shown and limited after three failures`() = runTest {
        val device = NearbyDevice("nearby-1", "新手机", DiscoveryMode.PAIR)
        val repository = FakeDeviceTransferRepository(initialNearbyDevices = listOf(device)).apply {
            pairingResult = PairingResult.Failure(TransferFailureCode.PAIRING_FAILED)
        }
        val viewModel = DevicesViewModel(repository, "本机", "fingerprint")
        advanceUntilIdle()

        viewModel.onAction(DevicesAction.SelectNearbyDevice(device))
        viewModel.onAction(DevicesAction.UpdatePairingCode("123456"))
        repeat(3) {
            viewModel.onAction(DevicesAction.SubmitPairingCode)
            advanceUntilIdle()
        }

        val dialog = checkNotNull(viewModel.uiState.value.pairingDialog)
        assertThat(dialog.failureCount).isEqualTo(3)
        assertThat(dialog.isLimited).isTrue()
        assertThat(dialog.errorMessage).isEqualTo("配对失败次数过多，请重新获取配对码。")
    }

    @Test
    fun `paired identity confirmation can be accepted or rejected without keeping trust`() = runTest {
        val device = NearbyDevice("nearby-1", "新手机", DiscoveryMode.PAIR)
        val trusted = TrustedDevice("trusted-1", "新手机", "AA:BB:CC")
        val repository = FakeDeviceTransferRepository(
            initialNearbyDevices = listOf(device),
            initialTrustedDevices = listOf(trusted),
        )
        val viewModel = DevicesViewModel(repository, "本机", "LOCAL")
        advanceUntilIdle()

        viewModel.onAction(DevicesAction.SelectNearbyDevice(device))
        viewModel.onAction(DevicesAction.UpdatePairingCode("123456"))
        viewModel.onAction(DevicesAction.SubmitPairingCode)
        advanceUntilIdle()

        val confirmation = checkNotNull(viewModel.uiState.value.pairingConfirmation)
        assertThat(confirmation.peerFingerprint).isEqualTo("AA:BB:CC")

        viewModel.onAction(DevicesAction.RejectPairingIdentity)
        advanceUntilIdle()

        assertThat(repository.forgottenDeviceIds).containsExactly("trusted-1")
        assertThat(viewModel.uiState.value.pairingConfirmation).isNull()
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h800dp")
class DevicesPairingScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `pairing dialog accepts code input and submit action`() {
        val actions = mutableListOf<DevicesAction>()
        val device = NearbyDevice("nearby-1", "新手机", DiscoveryMode.PAIR)

        composeRule.setContent {
            BanduTijiTheme {
                DevicesScreen(
                    uiState = DevicesUiState(
                        localDeviceName = "本机",
                        localFingerprint = "fingerprint",
                        nearbyDevices = listOf(device),
                        pairingDialog = PairingDialogUiState(device = device),
                        isLoading = false,
                    ),
                    onAction = actions::add,
                )
            }
        }

        composeRule.onNodeWithTag(PAIRING_DIALOG_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(PAIRING_CODE_FIELD_TAG).performTextReplacement("123456")
        composeRule.onNodeWithTag(PAIRING_SUBMIT_TAG).performClick()
        composeRule.onNodeWithText("配对 新手机").assertIsDisplayed()

        composeRule.runOnIdle {
            assertThat(actions).containsAtLeast(
                DevicesAction.UpdatePairingCode("123456"),
                DevicesAction.SubmitPairingCode,
            )
        }
    }

    @Test
    fun `pairing confirmation shows both fingerprints and reject action`() {
        val actions = mutableListOf<DevicesAction>()

        composeRule.setContent {
            BanduTijiTheme {
                DevicesScreen(
                    uiState = DevicesUiState(
                        localDeviceName = "本机",
                        localFingerprint = "LOCAL",
                        pairingConfirmation = PairingConfirmationUiState(
                            deviceName = "新手机",
                            localFingerprint = "LOCAL",
                            peerFingerprint = "AA:BB:CC",
                            trustedDeviceId = "trusted-1",
                        ),
                        isLoading = false,
                    ),
                    onAction = actions::add,
                )
            }
        }

        composeRule.onNodeWithTag(PAIRING_CONFIRMATION_TAG).assertIsDisplayed()
        composeRule.onNodeWithText("本机身份：LOCAL").assertIsDisplayed()
        composeRule.onNodeWithText("对方身份：AA:BB:CC").assertIsDisplayed()
        composeRule.onNodeWithText("身份不一致，取消配对")
            .performScrollTo()
            .assertIsDisplayed()
    }
}
