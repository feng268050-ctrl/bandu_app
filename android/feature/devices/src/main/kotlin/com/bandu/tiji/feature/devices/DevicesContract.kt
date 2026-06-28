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
    val isDiscoveryCommandRunning: Boolean = false,
    val commandErrorMessage: String? = null,
    val receiveMode: ReceiveModeUiState? = null,
    val pairingDialog: PairingDialogUiState? = null,
    val pairingConfirmation: PairingConfirmationUiState? = null,
    val forgetDevice: ForgetDeviceUiState? = null,
    val sendConfirmation: SendConfirmationUiState? = null,
)

data class ReceiveModeUiState(
    val code: String?,
    val expiresAtEpochMillis: Long?,
    val secondsRemaining: Int,
    val isCreating: Boolean,
    val errorMessage: String? = null,
) {
    val isExpired: Boolean
        get() = code != null && secondsRemaining <= 0
}

data class PairingDialogUiState(
    val device: NearbyDevice,
    val code: String = "",
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val failureCount: Int = 0,
) {
    val isLimited: Boolean
        get() = failureCount >= 3
}

data class PairingConfirmationUiState(
    val deviceName: String,
    val localFingerprint: String,
    val peerFingerprint: String,
    val trustedDeviceId: String? = null,
    val isRejecting: Boolean = false,
    val errorMessage: String? = null,
)

data class ForgetDeviceUiState(
    val device: TrustedDevice,
    val isForgetting: Boolean = false,
    val errorMessage: String? = null,
)

data class SendConfirmationUiState(
    val target: TrustedDevice,
    val isSending: Boolean = false,
    val errorMessage: String? = null,
)

sealed interface DevicesAction {
    data object Retry : DevicesAction

    data object StartDiscovery : DevicesAction

    data object StopDiscovery : DevicesAction

    data object LeavePage : DevicesAction

    data object StartReceiveMode : DevicesAction

    data object StopReceiveMode : DevicesAction

    data class SelectNearbyDevice(
        val device: NearbyDevice,
    ) : DevicesAction

    data class UpdatePairingCode(
        val code: String,
    ) : DevicesAction

    data object SubmitPairingCode : DevicesAction

    data object DismissPairing : DevicesAction

    data object ConfirmPairingIdentity : DevicesAction

    data object RejectPairingIdentity : DevicesAction

    data class RequestForgetDevice(
        val device: TrustedDevice,
    ) : DevicesAction

    data object ConfirmForgetDevice : DevicesAction

    data object DismissForgetDevice : DevicesAction

    data class RequestSendAllData(
        val target: TrustedDevice,
    ) : DevicesAction

    data object ConfirmSendAllData : DevicesAction

    data object DismissSendAllData : DevicesAction

    data object AcceptIncomingTransfer : DevicesAction

    data object RejectIncomingTransfer : DevicesAction
}

sealed interface DevicesEffect
