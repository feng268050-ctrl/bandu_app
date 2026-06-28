package com.bandu.tiji.feature.devices

import com.bandu.tiji.core.model.transfer.TransferPhase
import com.bandu.tiji.core.model.transfer.TransferProgress
import com.bandu.tiji.core.testing.coroutines.MainDispatcherRule
import com.bandu.tiji.core.testing.fake.FakeDeviceTransferRepository
import com.bandu.tiji.domain.transfer.DiscoveryMode
import com.bandu.tiji.domain.transfer.NearbyDevice
import com.bandu.tiji.domain.transfer.PairingCode
import com.bandu.tiji.domain.transfer.TransferOfferSummary
import com.bandu.tiji.domain.transfer.TransferState
import com.bandu.tiji.domain.transfer.TransferSummary
import com.bandu.tiji.domain.transfer.TrustedDevice
import com.google.common.truth.Truth.assertThat
import java.io.File
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DevicesFakeFlowTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `complete fake repository flow uses only domain transfer contracts`() = runTest {
        val nearby = NearbyDevice("nearby-1", "新手机", DiscoveryMode.PAIR)
        val trusted = TrustedDevice("trusted-1", "新手机", "AA:BB:CC")
        val offer = TransferOfferSummary(
            sessionId = "session-1",
            sourceDeviceName = "旧手机",
            totalBytes = 5_242_880L,
            totalFiles = 12,
            totalRecords = 300,
        )
        val repository = FakeDeviceTransferRepository(
            initialNearbyDevices = listOf(nearby),
            initialTrustedDevices = listOf(trusted),
        ).apply {
            pairingCode = PairingCode("654321", Long.MAX_VALUE)
        }
        val viewModel = DevicesViewModel(repository, "本机", "LOCAL")
        advanceUntilIdle()

        viewModel.onAction(DevicesAction.StartDiscovery)
        advanceUntilIdle()
        viewModel.onAction(DevicesAction.StartReceiveMode)
        runCurrent()
        viewModel.onAction(DevicesAction.StopReceiveMode)
        advanceUntilIdle()
        viewModel.onAction(DevicesAction.SelectNearbyDevice(nearby))
        viewModel.onAction(DevicesAction.UpdatePairingCode("654321"))
        viewModel.onAction(DevicesAction.SubmitPairingCode)
        advanceUntilIdle()
        viewModel.onAction(DevicesAction.ConfirmPairingIdentity)
        viewModel.onAction(DevicesAction.RequestSendAllData(trusted))
        viewModel.onAction(DevicesAction.ConfirmSendAllData)
        advanceUntilIdle()

        repository.emitTransferState(TransferState.AwaitingOfferConfirmation(offer))
        advanceUntilIdle()
        viewModel.onAction(DevicesAction.AcceptIncomingTransfer)
        advanceUntilIdle()
        repository.emitTransferState(
            TransferState.Transferring(
                TransferProgress(
                    phase = TransferPhase.TRANSFERRING,
                    percentComplete = 40,
                    transferredBytes = 2_097_152L,
                    totalBytes = 5_242_880L,
                    bytesPerSecond = 1_048_576L,
                ),
            ),
        )
        advanceUntilIdle()
        repository.emitTransferState(TransferState.AwaitingFinalConfirmation)
        advanceUntilIdle()
        viewModel.onAction(DevicesAction.ConfirmFinalTransfer)
        advanceUntilIdle()
        repository.emitTransferState(
            TransferState.Completed(
                TransferSummary("session-1", 5_242_880L, 5_000L),
            ),
        )
        advanceUntilIdle()

        assertThat(repository.startDiscoveryCalls).isEqualTo(1)
        assertThat(repository.createReceiveCodeCalls).isEqualTo(1)
        assertThat(repository.pairRequests.single().code).isEqualTo("654321")
        assertThat(repository.sendTargets).containsExactly(trusted)
        assertThat(repository.acceptedSessionIds).containsExactly("session-1", "session-1")
        assertThat(viewModel.uiState.value.transferState)
            .isInstanceOf(TransferState.Completed::class.java)
    }

    @Test
    fun `feature devices source does not reference nsd or socket runtime types`() {
        val moduleRoot = findAndroidRoot().resolve("feature/devices/src/main")
        val forbidden = listOf("NsdManager", "java.net.Socket", "ServerSocket", "DatagramSocket")
        val source = moduleRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .joinToString(separator = "\n") { it.readText() }

        forbidden.forEach { token ->
            assertThat(source).doesNotContain(token)
        }
    }

    private fun findAndroidRoot(): File {
        var current = File(requireNotNull(System.getProperty("user.dir"))).canonicalFile
        while (true) {
            if (current.resolve("settings.gradle.kts").isFile &&
                current.resolve("feature/devices").isDirectory
            ) {
                return current
            }
            current = current.parentFile
                ?: error("Unable to locate the Android project root")
        }
    }
}
