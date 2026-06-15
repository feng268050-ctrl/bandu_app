package com.bandu.tiji.domain.repository

import com.bandu.tiji.domain.transfer.NearbyDevice
import com.bandu.tiji.domain.transfer.PairingCode
import com.bandu.tiji.domain.transfer.PairingResult
import com.bandu.tiji.domain.transfer.TransferState
import com.bandu.tiji.domain.transfer.TrustedDevice
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface DeviceTransferRepository {
    fun observeNearbyDevices(): Flow<List<NearbyDevice>>

    fun observeTrustedDevices(): Flow<List<TrustedDevice>>

    fun observeTransferState(): StateFlow<TransferState>

    suspend fun startDiscovery()

    suspend fun stopDiscovery()

    suspend fun createReceiveCode(): PairingCode

    suspend fun pair(device: NearbyDevice, code: String): PairingResult

    suspend fun sendAll(target: TrustedDevice)

    suspend fun acceptTransfer(sessionId: String)

    suspend fun rejectTransfer(sessionId: String)

    suspend fun cancelTransfer()

    suspend fun forgetDevice(deviceId: String)
}
