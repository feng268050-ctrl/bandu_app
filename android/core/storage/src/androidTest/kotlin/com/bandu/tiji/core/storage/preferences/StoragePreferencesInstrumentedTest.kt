package com.bandu.tiji.core.storage.preferences

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bandu.tiji.core.model.enums.AiProviderType
import com.google.common.truth.Truth.assertThat
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StoragePreferencesInstrumentedTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val root = File(context.cacheDir, "storage-preferences-test-${System.nanoTime()}")

    @After
    fun tearDown() {
        scope.cancel()
        root.deleteRecursively()
    }

    @Test
    fun portableMigration_doesNotReplaceDeviceOnlyFields() = runBlocking {
        val sourcePortable = StoragePreferencesFactory.createPortable(File(root, "source/slot_a"), scope)
        val sourceDevice = StoragePreferencesFactory.createDevice(File(root, "source/device"), scope)
        val targetPortable = StoragePreferencesFactory.createPortable(File(root, "target/slot_b"), scope)
        val targetDevice = StoragePreferencesFactory.createDevice(File(root, "target/device"), scope)
        val portableValue = PortablePreferences(
            nickname = "小伴",
            educationStage = "junior_high",
            enrollmentYear = 2025,
            providerType = AiProviderType.OPENAI_COMPATIBLE,
            providerDisplayName = "家庭服务",
            baseUrl = "https://example.test/v1",
            analysisModel = "vision",
            tutorModel = "tutor",
            analyzeImagePrompt = "analyze",
            tutorPrompt = "tutor",
            generateExercisePrompt = "exercise",
            gradeExercisePrompt = "grade",
            promptSchemaVersion = 3,
        )
        val sourceDeviceValue = DevicePreferences(
            deviceId = "source-device",
            activeSlot = "slot_a",
            trustedPeerRecords = setOf("source-peer"),
            identityKeyAlias = "source-identity",
            allowPrivateHttp = true,
            pendingCommitSlot = "slot_b",
        )
        val targetDeviceValue = DevicePreferences(
            deviceId = "target-device",
            activeSlot = "slot_b",
            trustedPeerRecords = setOf("target-peer"),
            identityKeyAlias = "target-identity",
            allowPrivateHttp = false,
        )
        sourcePortable.replace(portableValue)
        sourceDevice.replace(sourceDeviceValue)
        targetDevice.replace(targetDeviceValue)

        targetPortable.replace(sourcePortable.data.first())

        assertThat(targetPortable.data.first()).isEqualTo(portableValue)
        assertThat(sourceDevice.data.first()).isEqualTo(sourceDeviceValue)
        assertThat(targetDevice.data.first()).isEqualTo(targetDeviceValue)
        assertThat(File(root, "source/slot_a/${StoragePreferencesFactory.PORTABLE_FILE_NAME}").isFile)
            .isTrue()
        assertThat(File(root, "source/device/${StoragePreferencesFactory.DEVICE_FILE_NAME}").isFile)
            .isTrue()
    }
}
