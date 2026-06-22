package com.bandu.tiji.feature.devices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bandu.tiji.domain.repository.DeviceTransferRepository
import com.bandu.tiji.core.common.result.AppResult
import com.bandu.tiji.domain.usecase.transfer.StartDiscoveryUseCase
import com.bandu.tiji.domain.usecase.transfer.StopDiscoveryUseCase
import kotlinx.coroutines.Job
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
    private val startDiscovery = StartDiscoveryUseCase(repository)
    private val stopDiscovery = StopDiscoveryUseCase(repository)

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
}
