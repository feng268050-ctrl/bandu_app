package com.bandu.tiji.feature.devices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bandu.tiji.domain.repository.DeviceTransferRepository
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

    init {
        observeDevices()
    }

    fun onAction(action: DevicesAction) {
        when (action) {
            DevicesAction.Retry -> observeDevices()
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
}
