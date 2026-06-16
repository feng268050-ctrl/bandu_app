package com.bandu.tiji.core.storage.preferences

import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.io.File
import kotlinx.coroutines.CoroutineScope

object StoragePreferencesFactory {
    const val PORTABLE_FILE_NAME = "portable.preferences_pb"
    const val DEVICE_FILE_NAME = "device.preferences_pb"

    fun createPortable(
        slotDirectory: File,
        scope: CoroutineScope,
    ): PortablePreferencesStore = PortablePreferencesStore(
        createStore(File(slotDirectory, PORTABLE_FILE_NAME), scope),
    )

    fun createDevice(
        deviceDirectory: File,
        scope: CoroutineScope,
    ): DevicePreferencesStore = DevicePreferencesStore(
        createStore(File(deviceDirectory, DEVICE_FILE_NAME), scope),
    )

    private fun createStore(
        file: File,
        scope: CoroutineScope,
    ) = PreferenceDataStoreFactory.create(
        scope = scope,
        produceFile = {
            require(file.parentFile?.let { it.exists() || it.mkdirs() } != false) {
                "Unable to create preferences directory"
            }
            file
        },
    )
}
