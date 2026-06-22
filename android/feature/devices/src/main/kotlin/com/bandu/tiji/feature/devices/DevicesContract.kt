package com.bandu.tiji.feature.devices

import com.bandu.tiji.domain.transfer.NearbyDevice
import com.bandu.tiji.domain.transfer.TransferState
import com.bandu.tiji.domain.transfer.TrustedDevice

data class DevicesUiState(
    val localDeviceName: String,
    val localFingerprint: String,
    val nearbyDevices: List<NearbyDevice> = emptyList(),
    val trustedDevices: List<TrustedDevice> = emptyList(),
    val transferState: TransferState = TransferState.Idle,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)

sealed interface DevicesAction {
    data object Retry : DevicesAction
}

sealed interface DevicesEffect
