package com.bandu.tiji.domain.usecase

import com.bandu.tiji.domain.fake.FakeDeviceTransferRepository
import com.bandu.tiji.domain.transfer.DiscoveryMode
import com.bandu.tiji.domain.transfer.NearbyDevice
import com.bandu.tiji.domain.transfer.PairingResult
import com.bandu.tiji.domain.transfer.TransferFailureCode
import com.bandu.tiji.domain.transfer.TransferState
import com.bandu.tiji.domain.transfer.TrustedDevice
import com.bandu.tiji.domain.usecase.transfer.AcceptTransferUseCase
import com.bandu.tiji.domain.usecase.transfer.CancelTransferUseCase
import com.bandu.tiji.domain.usecase.transfer.CreateReceiveCodeUseCase
import com.bandu.tiji.domain.usecase.transfer.ForgetDeviceUseCase
import com.bandu.tiji.domain.usecase.transfer.PairDeviceUseCase
import com.bandu.tiji.domain.usecase.transfer.RejectTransferUseCase
import com.bandu.tiji.domain.usecase.transfer.SendAllDataUseCase
import com.bandu.tiji.domain.usecase.transfer.StartDiscoveryUseCase
import com.bandu.tiji.domain.usecase.transfer.StopDiscoveryUseCase
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

class TransferUseCaseTest {
    private val repository = FakeDeviceTransferRepository()

    @Test
    fun startDiscovery_updatesState() = runTest {
        val result = StartDiscoveryUseCase(repository).invoke()

        assertThat(result.isSuccess).isTrue()
        assertThat(repository.discoveryStarted).isTrue()
        assertThat(repository.observeTransferState().value).isEqualTo(TransferState.Discovering)
    }

    @Test
    fun stopDiscovery_returnsToIdle() = runTest {
        repository.setTransferState(TransferState.Discovering)

        val result = StopDiscoveryUseCase(repository).invoke()

        assertThat(result.isSuccess).isTrue()
        assertThat(repository.discoveryStopped).isTrue()
        assertThat(repository.observeTransferState().value).isEqualTo(TransferState.Idle)
    }

    @Test
    fun createReceiveCode_returnsRepositoryValue() = runTest {
        val result = CreateReceiveCodeUseCase(repository).invoke()

        assertThat(result.getOrNull()?.code).isEqualTo("123456")
    }

    @Test
    fun pairDevice_rejectsInvalidCode() = runTest {
        val device = NearbyDevice("peer-1", "Phone B", DiscoveryMode.PAIR)
        val result = PairDeviceUseCase(repository).invoke(device, "abc")

        assertThat(result.isSuccess).isFalse()
    }

    @Test
    fun pairDevice_mapsRepositoryFailure() = runTest {
        repository.pairingResult = PairingResult.Failure(TransferFailureCode.PAIRING_EXPIRED)
        val device = NearbyDevice("peer-1", "Phone B", DiscoveryMode.PAIR)

        val result = PairDeviceUseCase(repository).invoke(device, "123456")

        assertThat(result.isSuccess).isFalse()
    }

    @Test
    fun sendAllData_delegatesToRepository() = runTest {
        val target = TrustedDevice("d1", "Phone B", "fp")
        val result = SendAllDataUseCase(repository).invoke(target)

        assertThat(result.isSuccess).isTrue()
        assertThat(repository.sendTargets).containsExactly(target)
    }

    @Test
    fun acceptAndRejectTransfer_recordSessionIds() = runTest {
        AcceptTransferUseCase(repository).invoke("session-1")
        RejectTransferUseCase(repository).invoke("session-2")

        assertThat(repository.acceptedSessions).containsExactly("session-1")
        assertThat(repository.rejectedSessions).containsExactly("session-2")
    }

    @Test
    fun cancelTransfer_delegatesToRepository() = runTest {
        val result = CancelTransferUseCase(repository).invoke()

        assertThat(result.isSuccess).isTrue()
        assertThat(repository.cancelled).isTrue()
    }

    @Test
    fun forgetDevice_delegatesToRepository() = runTest {
        val result = ForgetDeviceUseCase(repository).invoke("device-1")

        assertThat(result.isSuccess).isTrue()
        assertThat(repository.forgottenDeviceIds).containsExactly("device-1")
    }
}
