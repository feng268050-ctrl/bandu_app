package com.bandu.tiji.feature.devices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bandu.tiji.domain.repository.DeviceTransferRepository
import com.bandu.tiji.core.common.result.AppResult
import com.bandu.tiji.core.common.time.Clock
import com.bandu.tiji.core.common.time.SystemClock
import com.bandu.tiji.domain.transfer.PairingResult
import com.bandu.tiji.domain.transfer.TransferFailureCode
import com.bandu.tiji.domain.transfer.TransferState
import com.bandu.tiji.domain.usecase.transfer.AcceptTransferUseCase
import com.bandu.tiji.domain.usecase.transfer.CancelTransferUseCase
import com.bandu.tiji.domain.usecase.transfer.CreateReceiveCodeUseCase
import com.bandu.tiji.domain.usecase.transfer.ForgetDeviceUseCase
import com.bandu.tiji.domain.usecase.transfer.RejectTransferUseCase
import com.bandu.tiji.domain.usecase.transfer.SendAllDataUseCase
import com.bandu.tiji.domain.usecase.transfer.StartDiscoveryUseCase
import com.bandu.tiji.domain.usecase.transfer.StopDiscoveryUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class DevicesViewModel(
    private val repository: DeviceTransferRepository,
    localDeviceName: String,
    localFingerprint: String,
    private val clock: Clock = SystemClock(),
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(
        DevicesUiState(
            localDeviceName = localDeviceName,
            localFingerprint = localFingerprint,
        ),
    )
    val uiState: StateFlow<DevicesUiState> = mutableUiState.asStateFlow()
    private var observationJob: Job? = null
    private var commandJob: Job? = null
    private var receiveJob: Job? = null
    private val startDiscovery = StartDiscoveryUseCase(repository)
    private val stopDiscovery = StopDiscoveryUseCase(repository)
    private val createReceiveCode = CreateReceiveCodeUseCase(repository)
    private val forgetDevice = ForgetDeviceUseCase(repository)
    private val sendAllData = SendAllDataUseCase(repository)
    private val acceptTransfer = AcceptTransferUseCase(repository)
    private val rejectTransfer = RejectTransferUseCase(repository)
    private val cancelTransfer = CancelTransferUseCase(repository)
    private var activeSessionId: String? = null

    init {
        observeDevices()
    }

    fun onAction(action: DevicesAction) {
        when (action) {
            DevicesAction.Retry -> observeDevices()
            DevicesAction.StartDiscovery -> runDiscoveryCommand(start = true)
            DevicesAction.StopDiscovery,
            DevicesAction.LeavePage,
            -> runDiscoveryCommand(start = false)
            DevicesAction.StartReceiveMode -> startReceiveMode()
            DevicesAction.StopReceiveMode -> stopReceiveMode()
            is DevicesAction.SelectNearbyDevice -> {
                mutableUiState.value = mutableUiState.value.copy(
                    pairingDialog = PairingDialogUiState(device = action.device),
                )
            }
            is DevicesAction.UpdatePairingCode -> {
                val current = mutableUiState.value.pairingDialog ?: return
                mutableUiState.value = mutableUiState.value.copy(
                    pairingDialog = current.copy(
                        code = action.code.filter(Char::isDigit).take(6),
                        errorMessage = null,
                    ),
                )
            }
            DevicesAction.SubmitPairingCode -> submitPairingCode()
            DevicesAction.DismissPairing -> {
                mutableUiState.value = mutableUiState.value.copy(pairingDialog = null)
            }
            DevicesAction.ConfirmPairingIdentity -> {
                mutableUiState.value = mutableUiState.value.copy(pairingConfirmation = null)
            }
            DevicesAction.RejectPairingIdentity -> rejectPairingIdentity()
            is DevicesAction.RequestForgetDevice -> {
                mutableUiState.value = mutableUiState.value.copy(
                    forgetDevice = ForgetDeviceUiState(action.device),
                )
            }
            DevicesAction.ConfirmForgetDevice -> confirmForgetDevice()
            DevicesAction.DismissForgetDevice -> {
                mutableUiState.value = mutableUiState.value.copy(forgetDevice = null)
            }
            is DevicesAction.RequestSendAllData -> {
                mutableUiState.value = mutableUiState.value.copy(
                    sendConfirmation = SendConfirmationUiState(action.target),
                )
            }
            DevicesAction.ConfirmSendAllData -> confirmSendAllData()
            DevicesAction.DismissSendAllData -> {
                mutableUiState.value = mutableUiState.value.copy(sendConfirmation = null)
            }
            DevicesAction.AcceptIncomingTransfer -> acceptIncomingTransfer()
            DevicesAction.RejectIncomingTransfer -> rejectIncomingTransfer()
            DevicesAction.ConfirmFinalTransfer -> confirmFinalTransfer()
            DevicesAction.CancelTransfer -> cancelTransfer()
        }
    }

    private fun observeDevices() {
        observationJob?.cancel()
        mutableUiState.value = mutableUiState.value.copy(
            isLoading = true,
            errorMessage = null,
        )
        observationJob = viewModelScope.launch {
            combine(
                repository.observeNearbyDevices(),
                repository.observeTrustedDevices(),
                repository.observeTransferState(),
            ) { nearby, trusted, transferState ->
                Triple(nearby, trusted, transferState)
            }
                .catch {
                    mutableUiState.value = mutableUiState.value.copy(
                        isLoading = false,
                        errorMessage = "无法加载设备信息",
                    )
                }
                .collect { (nearby, trusted, transferState) ->
                    if (transferState is TransferState.AwaitingOfferConfirmation) {
                        activeSessionId = transferState.offer.sessionId
                    }
                    mutableUiState.value = mutableUiState.value.copy(
                        nearbyDevices = nearby,
                        trustedDevices = trusted,
                        transferState = transferState,
                        isLoading = false,
                        errorMessage = null,
                    )
                }
        }
    }

    private fun runDiscoveryCommand(start: Boolean) {
        if (mutableUiState.value.isDiscoveryCommandRunning) return
        mutableUiState.value = mutableUiState.value.copy(
            isDiscoveryCommandRunning = true,
            commandErrorMessage = null,
        )
        commandJob?.cancel()
        commandJob = viewModelScope.launch {
            val result = if (start) startDiscovery() else stopDiscovery()
            mutableUiState.value = mutableUiState.value.copy(
                isDiscoveryCommandRunning = false,
                commandErrorMessage = when (result) {
                    is AppResult.Success -> null
                    is AppResult.Failure ->
                        if (start) "无法开始发现设备" else "无法停止发现设备"
                },
            )
        }
    }

    private fun startReceiveMode() {
        if (mutableUiState.value.receiveMode?.isCreating == true) return
        mutableUiState.value = mutableUiState.value.copy(
            receiveMode = ReceiveModeUiState(
                code = null,
                expiresAtEpochMillis = null,
                secondsRemaining = 0,
                isCreating = true,
            ),
        )
        receiveJob?.cancel()
        receiveJob = viewModelScope.launch {
            when (val result = createReceiveCode()) {
                is AppResult.Success -> {
                    val code = result.value
                    updateReceiveCountdown(code.code, code.expiresAtEpochMillis)
                    while (mutableUiState.value.receiveMode?.isExpired == false) {
                        delay(1_000)
                        updateReceiveCountdown(code.code, code.expiresAtEpochMillis)
                    }
                }
                is AppResult.Failure -> mutableUiState.value = mutableUiState.value.copy(
                    receiveMode = ReceiveModeUiState(
                        code = null,
                        expiresAtEpochMillis = null,
                        secondsRemaining = 0,
                        isCreating = false,
                        errorMessage = "无法开启接收模式",
                    ),
                )
            }
        }
    }

    private fun updateReceiveCountdown(code: String, expiresAt: Long) {
        val remainingMillis = (expiresAt - clock.nowEpochMillis()).coerceAtLeast(0L)
        mutableUiState.value = mutableUiState.value.copy(
            receiveMode = ReceiveModeUiState(
                code = code,
                expiresAtEpochMillis = expiresAt,
                secondsRemaining = ((remainingMillis + 999L) / 1_000L)
                    .coerceAtMost(Int.MAX_VALUE.toLong())
                    .toInt(),
                isCreating = false,
            ),
        )
    }

    private fun stopReceiveMode() {
        receiveJob?.cancel()
        receiveJob = null
        mutableUiState.value = mutableUiState.value.copy(receiveMode = null)
        runDiscoveryCommand(start = false)
    }

    private fun submitPairingCode() {
        val current = mutableUiState.value.pairingDialog ?: return
        if (current.isSubmitting || current.isLimited) return
        if (!current.code.matches(Regex("\\d{6}"))) {
            mutableUiState.value = mutableUiState.value.copy(
                pairingDialog = current.copy(errorMessage = "请输入 6 位配对码"),
            )
            return
        }
        mutableUiState.value = mutableUiState.value.copy(
            pairingDialog = current.copy(isSubmitting = true, errorMessage = null),
        )
        commandJob?.cancel()
        commandJob = viewModelScope.launch {
            val result = runCatching { repository.pair(current.device, current.code) }
                .getOrElse { PairingResult.Failure(TransferFailureCode.PROTOCOL_ERROR) }
            when (result) {
                PairingResult.Success -> {
                    val trustedDevice = mutableUiState.value.trustedDevices
                        .firstOrNull { it.displayName == current.device.displayName }
                    mutableUiState.value = mutableUiState.value.copy(
                        pairingDialog = null,
                        pairingConfirmation = PairingConfirmationUiState(
                            deviceName = current.device.displayName,
                            localFingerprint = mutableUiState.value.localFingerprint,
                            peerFingerprint = trustedDevice?.publicKeyFingerprint
                                ?: current.device.discoveryId,
                            trustedDeviceId = trustedDevice?.deviceId,
                        ),
                    )
                }
                is PairingResult.Failure -> {
                    val failureCount = current.failureCount + 1
                    mutableUiState.value = mutableUiState.value.copy(
                        pairingDialog = current.copy(
                            isSubmitting = false,
                            failureCount = failureCount,
                            errorMessage = if (failureCount >= 3) {
                                "配对失败次数过多，请重新获取配对码。"
                            } else {
                                result.code.toPairingErrorMessage()
                            },
                        ),
                    )
                }
            }
        }
    }

    private fun TransferFailureCode.toPairingErrorMessage(): String =
        when (this) {
            TransferFailureCode.PAIRING_EXPIRED -> "配对码已过期，请重新生成后再试。"
            TransferFailureCode.PAIRING_FAILED -> "配对码错误，请检查后重试。"
            TransferFailureCode.NETWORK_INTERRUPTED -> "网络连接中断，请重试。"
            else -> "配对失败，请重试。"
        }

    private fun rejectPairingIdentity() {
        val confirmation = mutableUiState.value.pairingConfirmation ?: return
        if (confirmation.isRejecting) return
        val trustedDeviceId = confirmation.trustedDeviceId
        if (trustedDeviceId == null) {
            mutableUiState.value = mutableUiState.value.copy(pairingConfirmation = null)
            return
        }
        mutableUiState.value = mutableUiState.value.copy(
            pairingConfirmation = confirmation.copy(isRejecting = true, errorMessage = null),
        )
        commandJob?.cancel()
        commandJob = viewModelScope.launch {
            when (forgetDevice(trustedDeviceId)) {
                is AppResult.Success -> mutableUiState.value = mutableUiState.value.copy(
                    pairingConfirmation = null,
                )
                is AppResult.Failure -> mutableUiState.value = mutableUiState.value.copy(
                    pairingConfirmation = confirmation.copy(
                        isRejecting = false,
                        errorMessage = "无法取消信任，请稍后重试。",
                    ),
                )
            }
        }
    }

    private fun confirmForgetDevice() {
        val pending = mutableUiState.value.forgetDevice ?: return
        if (pending.isForgetting) return
        mutableUiState.value = mutableUiState.value.copy(
            forgetDevice = pending.copy(isForgetting = true, errorMessage = null),
        )
        commandJob?.cancel()
        commandJob = viewModelScope.launch {
            when (forgetDevice(pending.device.deviceId)) {
                is AppResult.Success -> mutableUiState.value = mutableUiState.value.copy(
                    forgetDevice = null,
                )
                is AppResult.Failure -> mutableUiState.value = mutableUiState.value.copy(
                    forgetDevice = pending.copy(
                        isForgetting = false,
                        errorMessage = "无法解除配对，请稍后重试。",
                    ),
                )
            }
        }
    }

    private fun confirmSendAllData() {
        val pending = mutableUiState.value.sendConfirmation ?: return
        if (pending.isSending) return
        mutableUiState.value = mutableUiState.value.copy(
            sendConfirmation = pending.copy(isSending = true, errorMessage = null),
        )
        commandJob?.cancel()
        commandJob = viewModelScope.launch {
            when (sendAllData(pending.target)) {
                is AppResult.Success -> mutableUiState.value = mutableUiState.value.copy(
                    sendConfirmation = null,
                )
                is AppResult.Failure -> mutableUiState.value = mutableUiState.value.copy(
                    sendConfirmation = pending.copy(
                        isSending = false,
                        errorMessage = "无法开始迁移，请稍后重试。",
                    ),
                )
            }
        }
    }

    private fun acceptIncomingTransfer() {
        val offer = (mutableUiState.value.transferState as? TransferState.AwaitingOfferConfirmation)
            ?.offer ?: return
        commandJob?.cancel()
        commandJob = viewModelScope.launch {
            val message = when (acceptTransfer(offer.sessionId)) {
                is AppResult.Success -> null
                is AppResult.Failure -> "无法接收迁移，请稍后重试。"
            }
            mutableUiState.value = mutableUiState.value.copy(commandErrorMessage = message)
        }
    }

    private fun rejectIncomingTransfer() {
        val offer = (mutableUiState.value.transferState as? TransferState.AwaitingOfferConfirmation)
            ?.offer ?: return
        commandJob?.cancel()
        commandJob = viewModelScope.launch {
            val message = when (rejectTransfer(offer.sessionId)) {
                is AppResult.Success -> null
                is AppResult.Failure -> "无法拒绝迁移，请稍后重试。"
            }
            mutableUiState.value = mutableUiState.value.copy(commandErrorMessage = message)
        }
    }

    private fun confirmFinalTransfer() {
        val sessionId = activeSessionId ?: return
        commandJob?.cancel()
        commandJob = viewModelScope.launch {
            val message = when (acceptTransfer(sessionId)) {
                is AppResult.Success -> null
                is AppResult.Failure -> "无法确认迁移，请稍后重试。"
            }
            mutableUiState.value = mutableUiState.value.copy(commandErrorMessage = message)
        }
    }

    private fun cancelTransfer() {
        commandJob?.cancel()
        commandJob = viewModelScope.launch {
            val message = when (cancelTransfer.invoke()) {
                is AppResult.Success -> null
                is AppResult.Failure -> "无法取消迁移，请稍后重试。"
            }
            mutableUiState.value = mutableUiState.value.copy(commandErrorMessage = message)
        }
    }
}
