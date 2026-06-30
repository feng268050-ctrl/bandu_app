package com.bandu.tiji.data.repository

import com.bandu.tiji.domain.repository.DeviceTransferRepository
import com.bandu.tiji.domain.transfer.NearbyDevice
import com.bandu.tiji.domain.transfer.PairingCode
import com.bandu.tiji.domain.transfer.PairingResult
import com.bandu.tiji.domain.transfer.TransferState
import com.bandu.tiji.domain.transfer.TrustedDevice
import com.bandu.tiji.transfer.runtime.TransferRuntime
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

@Singleton
class RuntimeDeviceTransferRepository @Inject constructor(
    private val runtime: TransferRuntime,
) : DeviceTransferRepository {
    override fun observeNearbyDevices(): StateFlow<List<NearbyDevice>> =
        runtime.observeNearbyDevices()

    override fun observeTrustedDevices(): Flow<List<TrustedDevice>> =
        runtime.observeTrustedDevices()

    override fun observeTransferState(): StateFlow<TransferState> =
        runtime.observeTransferState()

    override suspend fun startDiscovery() {
        runtime.startDiscovery()
    }

    override suspend fun stopDiscovery() {
        runtime.stopDiscovery()
    }

    override suspend fun createReceiveCode(): PairingCode =
        runtime.createReceiveCode()

    override suspend fun pair(
        device: NearbyDevice,
        code: String,
    ): PairingResult =
        runtime.pair(device, code)

    override suspend fun sendAll(target: TrustedDevice) {
        runtime.sendAll(target)
    }

    override suspend fun acceptTransfer(sessionId: String) {
        runtime.acceptTransfer(sessionId)
    }

    override suspend fun rejectTransfer(sessionId: String) {
        runtime.rejectTransfer(sessionId)
    }

    override suspend fun cancelTransfer() {
        runtime.cancelTransfer()
    }

    override suspend fun forgetDevice(deviceId: String) {
        runtime.forgetDevice(deviceId)
    }
}
