package com.bandu.tiji.core.testing.fake

import com.bandu.tiji.domain.repository.DeviceTransferRepository
import com.bandu.tiji.domain.transfer.NearbyDevice
import com.bandu.tiji.domain.transfer.PairingCode
import com.bandu.tiji.domain.transfer.PairingResult
import com.bandu.tiji.domain.transfer.TransferState
import com.bandu.tiji.domain.transfer.TrustedDevice
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PairRequest(
    val device: NearbyDevice,
    val code: String,
)

class FakeDeviceTransferRepository(
    initialNearbyDevices: List<NearbyDevice> = emptyList(),
    initialTrustedDevices: List<TrustedDevice> = emptyList(),
    initialTransferState: TransferState = TransferState.Idle,
) : DeviceTransferRepository {
    private val nearbyDevices = MutableStateFlow(initialNearbyDevices.toList())
    private val trustedDevices = MutableStateFlow(initialTrustedDevices.toList())
    private val transferState = MutableStateFlow(initialTransferState)

    val failures = FailureInjector()
    val pairRequests = mutableListOf<PairRequest>()
    val sendTargets = mutableListOf<TrustedDevice>()
    val acceptedSessionIds = mutableListOf<String>()
    val rejectedSessionIds = mutableListOf<String>()
    val forgottenDeviceIds = mutableListOf<String>()
    var startDiscoveryCalls: Int = 0
        private set
    var stopDiscoveryCalls: Int = 0
        private set
    var createReceiveCodeCalls: Int = 0
        private set
    var cancelTransferCalls: Int = 0
        private set

    var pairingCode: PairingCode = PairingCode("123456", Long.MAX_VALUE)
    var pairingResult: PairingResult = PairingResult.Success

    override fun observeNearbyDevices(): Flow<List<NearbyDevice>> = nearbyDevices.asStateFlow()

    override fun observeTrustedDevices(): Flow<List<TrustedDevice>> = trustedDevices.asStateFlow()

    override fun observeTransferState(): StateFlow<TransferState> = transferState.asStateFlow()

    override suspend fun startDiscovery() {
        failures.throwIfQueued()
        startDiscoveryCalls += 1
        transferState.value = TransferState.Discovering
    }

    override suspend fun stopDiscovery() {
        failures.throwIfQueued()
        stopDiscoveryCalls += 1
        transferState.value = TransferState.Idle
    }

    override suspend fun createReceiveCode(): PairingCode {
        failures.throwIfQueued()
        createReceiveCodeCalls += 1
        return pairingCode
    }

    override suspend fun pair(
        device: NearbyDevice,
        code: String,
    ): PairingResult {
        failures.throwIfQueued()
        pairRequests += PairRequest(device, code)
        return pairingResult
    }

    override suspend fun sendAll(target: TrustedDevice) {
        failures.throwIfQueued()
        sendTargets += target
    }

    override suspend fun acceptTransfer(sessionId: String) {
        failures.throwIfQueued()
        acceptedSessionIds += sessionId
    }

    override suspend fun rejectTransfer(sessionId: String) {
        failures.throwIfQueued()
        rejectedSessionIds += sessionId
    }

    override suspend fun cancelTransfer() {
        failures.throwIfQueued()
        cancelTransferCalls += 1
    }

    override suspend fun forgetDevice(deviceId: String) {
        failures.throwIfQueued()
        forgottenDeviceIds += deviceId
        trustedDevices.value = trustedDevices.value.filterNot { it.deviceId == deviceId }
    }

    fun emitNearbyDevices(devices: List<NearbyDevice>) {
        nearbyDevices.value = devices.toList()
    }

    fun emitTrustedDevices(devices: List<TrustedDevice>) {
        trustedDevices.value = devices.toList()
    }

    fun emitTransferState(state: TransferState) {
        transferState.value = state
    }
}
