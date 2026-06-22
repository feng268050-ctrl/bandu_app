package com.bandu.tiji.feature.devices

import androidx.lifecycle.ViewModel
import com.bandu.tiji.domain.repository.DeviceTransferRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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

    fun onAction(action: DevicesAction) {
        when (action) {
            DevicesAction.Retry -> Unit
        }
    }
}
