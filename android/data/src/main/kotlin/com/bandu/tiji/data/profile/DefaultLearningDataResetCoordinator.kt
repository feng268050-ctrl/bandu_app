package com.bandu.tiji.data.profile

import com.bandu.tiji.core.common.id.UuidGenerator
import com.bandu.tiji.core.storage.db.LearningDatabase
import com.bandu.tiji.core.storage.preferences.DevicePreferences
import com.bandu.tiji.core.storage.preferences.DevicePreferencesStore
import com.bandu.tiji.core.storage.preferences.PortablePreferences
import com.bandu.tiji.core.storage.preferences.PortablePreferencesStore
import com.bandu.tiji.data.ai.ApiKeyCipherStore
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultLearningDataResetCoordinator @Inject constructor(
    private val database: LearningDatabase,
    private val portablePreferences: PortablePreferencesStore,
    private val devicePreferences: DevicePreferencesStore,
    private val apiKeyStore: ApiKeyCipherStore,
    private val uuidGenerator: UuidGenerator,
    private val resetPaths: ResetPaths,
) : LearningDataResetCoordinator {
    override suspend fun clearLearningData() {
        database.clearAllTables()
        resetPaths.imageRoot.deleteContentsAndRecreate()
    }

    override suspend fun factoryReset() {
        portablePreferences.replace(PortablePreferences())
        devicePreferences.replace(
            DevicePreferences(
                deviceId = uuidGenerator.newUuid(),
                deviceDisplayName = DEFAULT_DEVICE_NAME,
            ),
        )
        apiKeyStore.clear()
        database.close()
        resetPaths.slotsRoot.deleteRecursively()
        resetPaths.tempRoot.deleteRecursively()
        resetPaths.transferRoot.deleteRecursively()
    }

    private fun File.deleteContentsAndRecreate() {
        deleteRecursively()
        require(exists() || mkdirs()) { "Unable to recreate image directory" }
    }

    companion object {
        private const val DEFAULT_DEVICE_NAME = "伴读题集"
    }
}

data class ResetPaths(
    val slotsRoot: File,
    val imageRoot: File,
    val tempRoot: File,
    val transferRoot: File,
)
