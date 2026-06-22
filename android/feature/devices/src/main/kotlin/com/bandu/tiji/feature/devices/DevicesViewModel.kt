package com.bandu.tiji.feature.devices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bandu.tiji.domain.repository.DeviceTransferRepository
import com.bandu.tiji.core.common.result.AppResult
import com.bandu.tiji.core.common.time.Clock
import com.bandu.tiji.core.common.time.SystemClock
import com.bandu.tiji.domain.usecase.transfer.CreateReceiveCodeUseCase
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
}
