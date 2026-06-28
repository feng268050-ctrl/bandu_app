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
import com.bandu.tiji.domain.transfer.DiscoveryMode
import com.bandu.tiji.domain.transfer.NearbyDevice
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
class DevicesListViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `observes nearby trusted and local identities`() = runTest {
        val nearby = NearbyDevice("nearby-1", "新手机", DiscoveryMode.PAIR)
        val trusted = TrustedDevice("trusted-1", "平板", "11:22:33")
        val repository = FakeDeviceTransferRepository(
            initialNearbyDevices = listOf(nearby),
            initialTrustedDevices = listOf(trusted),
        )
        val viewModel = DevicesViewModel(repository, "本机", "AA:BB:CC")
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.localDeviceName).isEqualTo("本机")
        assertThat(viewModel.uiState.value.localFingerprint).isEqualTo("AA:BB:CC")
        assertThat(viewModel.uiState.value.nearbyDevices).containsExactly(nearby)
        assertThat(viewModel.uiState.value.trustedDevices).containsExactly(trusted)
        assertThat(viewModel.uiState.value.isLoading).isFalse()
    }

    @Test
    fun `forget trusted device uses confirmation before removing trust`() = runTest {
        val trusted = TrustedDevice("trusted-1", "平板", "11:22:33")
        val repository = FakeDeviceTransferRepository(initialTrustedDevices = listOf(trusted))
        val viewModel = DevicesViewModel(repository, "本机", "AA:BB:CC")
        advanceUntilIdle()

        viewModel.onAction(DevicesAction.RequestForgetDevice(trusted))
        assertThat(viewModel.uiState.value.forgetDevice?.device).isEqualTo(trusted)

        viewModel.onAction(DevicesAction.ConfirmForgetDevice)
        advanceUntilIdle()

        assertThat(repository.forgottenDeviceIds).containsExactly("trusted-1")
        assertThat(viewModel.uiState.value.forgetDevice).isNull()
        assertThat(viewModel.uiState.value.trustedDevices).isEmpty()
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h800dp")
class DevicesListScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `shows local nearby and trusted device details`() {
        composeRule.setContent {
            BanduTijiTheme {
                DevicesScreen(
                    uiState = DevicesUiState(
                        localDeviceName = "我的手机",
                        localFingerprint = "AA:BB:CC",
                        nearbyDevices = listOf(
                            NearbyDevice("nearby-1", "新手机", DiscoveryMode.PAIR),
                        ),
                        trustedDevices = listOf(
                            TrustedDevice("trusted-1", "平板", "11:22:33"),
                        ),
                        isLoading = false,
                    ),
                    onAction = {},
                )
            }
        }

        listOf("我的手机", "身份指纹：AA:BB:CC", "新手机", "平板").forEach { text ->
            composeRule.onNodeWithText(text).assertIsDisplayed()
        }
    }

    @Test
    fun `trusted device exposes forget confirmation`() {
        val trusted = TrustedDevice("trusted-1", "平板", "11:22:33")
        val actions = mutableListOf<DevicesAction>()
        composeRule.setContent {
            BanduTijiTheme {
                DevicesScreen(
                    uiState = DevicesUiState(
                        localDeviceName = "我的手机",
                        localFingerprint = "AA:BB:CC",
                        trustedDevices = listOf(trusted),
                        forgetDevice = ForgetDeviceUiState(trusted),
                        isLoading = false,
                    ),
                    onAction = actions::add,
                )
            }
        }

        composeRule.onNodeWithTag("devices-list").performScrollToIndex(6)
        composeRule.onNodeWithTag(FORGET_DEVICE_CONFIRMATION_TAG)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("解除后需要重新输入 6 位配对码才能迁移。")
            .assertIsDisplayed()
        composeRule.onNodeWithTag(FORGET_DEVICE_CONFIRM_TAG).performClick()

        composeRule.runOnIdle {
            assertThat(actions).contains(DevicesAction.ConfirmForgetDevice)
        }
    }
}
