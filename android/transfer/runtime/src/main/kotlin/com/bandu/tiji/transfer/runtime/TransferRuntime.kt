package com.bandu.tiji.transfer.runtime

import com.bandu.tiji.domain.transfer.DiscoveryMode
import com.bandu.tiji.domain.transfer.NearbyDevice
import com.bandu.tiji.domain.transfer.PairingCode
import com.bandu.tiji.domain.transfer.PairingResult
import com.bandu.tiji.domain.transfer.TransferFailureCode
import com.bandu.tiji.domain.transfer.TransferState
import com.bandu.tiji.domain.transfer.TransferSummary
import com.bandu.tiji.domain.transfer.TrustedDevice
import com.bandu.tiji.core.model.transfer.TransferPhase
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TransferRuntime {
    private val nearbyDevices = MutableStateFlow(
        listOf(NearbyDevice("peer-1", "伴读设备", DiscoveryMode.PAIR)),
    )
    private val trustedDevices = MutableStateFlow<List<TrustedDevice>>(emptyList())
    private val transferState = MutableStateFlow<TransferState>(TransferState.Idle)
    private val pairingCounter = AtomicInteger(1)

    fun observeNearbyDevices(): StateFlow<List<NearbyDevice>> = nearbyDevices.asStateFlow()

    fun observeTrustedDevices(): StateFlow<List<TrustedDevice>> = trustedDevices.asStateFlow()

    fun observeTransferState(): StateFlow<TransferState> = transferState.asStateFlow()

    fun startDiscovery() {
        transferState.value = TransferState.Discovering
    }

    fun stopDiscovery() {
        transferState.value = TransferState.Idle
    }

    fun createReceiveCode(): PairingCode =
        PairingCode(
            code = "12${pairingCounter.getAndIncrement().toString().padStart(4, '0')}",
            expiresAtEpochMillis = System.currentTimeMillis() + 5 * 60 * 1000,
        )

    fun pair(device: NearbyDevice, code: String): PairingResult {
        return if (code.length == 6) {
            val trusted = TrustedDevice(
                deviceId = device.discoveryId,
                displayName = device.displayName,
                publicKeyFingerprint = "fingerprint-${device.discoveryId}",
            )
            trustedDevices.value = trustedDevices.value + trusted
            PairingResult.Success
        } else {
            PairingResult.Failure(TransferFailureCode.PAIRING_FAILED)
        }
    }

    fun sendAll(target: TrustedDevice) {
        transferState.value = TransferState.Transferring(
            progress = com.bandu.tiji.core.model.transfer.TransferProgress(
                phase = TransferPhase.TRANSFERRING,
                percentComplete = 0,
                transferredBytes = 0L,
                totalBytes = 1L,
                bytesPerSecond = 0L,
            ),
        )
        transferState.value = TransferState.Completed(
            TransferSummary(
                sessionId = "session-${target.deviceId}",
                transferredBytes = 0L,
                durationMillis = 1L,
            ),
        )
    }

    fun acceptTransfer(sessionId: String) {
        transferState.value = TransferState.AwaitingFinalConfirmation
    }

    fun rejectTransfer(sessionId: String) {
        transferState.value = TransferState.Failed(TransferFailureCode.OFFER_REJECTED, resumable = false)
    }

    fun cancelTransfer() {
        transferState.value = TransferState.Failed(TransferFailureCode.CANCELLED, resumable = false)
    }

    fun forgetDevice(deviceId: String) {
        trustedDevices.value = trustedDevices.value.filterNot { it.deviceId == deviceId }
    }
}
