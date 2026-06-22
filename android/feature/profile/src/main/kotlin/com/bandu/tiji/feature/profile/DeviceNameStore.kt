package com.bandu.tiji.feature.profile

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

interface DeviceNameStore {
    fun observeDeviceName(): Flow<String>

    suspend fun saveDeviceName(name: String)
}

internal class InMemoryDeviceNameStore(
    initialName: String = "",
) : DeviceNameStore {
    private val name = MutableStateFlow(initialName)

    override fun observeDeviceName(): Flow<String> = name

    override suspend fun saveDeviceName(name: String) {
        this.name.value = name
    }
}
