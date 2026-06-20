package com.bandu.tiji.data.profile

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bandu.tiji.core.common.id.UuidGenerator
import com.bandu.tiji.core.model.enums.AiProviderType
import com.bandu.tiji.core.storage.db.LearningDatabase
import com.bandu.tiji.core.storage.db.entity.CollectionEntity
import com.bandu.tiji.core.storage.db.entity.ErrorItemEntity
import com.bandu.tiji.core.storage.preferences.DevicePreferences
import com.bandu.tiji.core.storage.preferences.PortablePreferences
import com.bandu.tiji.core.storage.preferences.StoragePreferencesFactory
import com.bandu.tiji.data.ai.ApiKeyCipherStore
import com.google.common.truth.Truth.assertThat
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DefaultLearningDataResetCoordinatorInstrumentedTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private lateinit var root: File
    private lateinit var scope: CoroutineScope
    private lateinit var database: LearningDatabase
    private lateinit var keyStore: FakeApiKeyCipherStore

    @Before
    fun setUp() {
        root = File(context.cacheDir, "data-reset-${System.nanoTime()}").apply { mkdirs() }
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        database = Room.inMemoryDatabaseBuilder(context, LearningDatabase::class.java).build()
        keyStore = FakeApiKeyCipherStore().apply { save("secret".toCharArray()) }
    }

    @After
    fun tearDown() {
        runCatching { database.close() }
        scope.cancel()
        root.deleteRecursively()
    }

    @Test
    fun clearLearningData_preservesProfileAiKeyAndDeviceState(): Unit = runBlocking {
        val portable = StoragePreferencesFactory.createPortable(File(root, "slots/slot_a"), scope)
        val device = StoragePreferencesFactory.createDevice(File(root, "device"), scope)
        val paths = paths()
        portable.replace(portableValue())
        device.replace(deviceValue())
        seedLearningData()
        File(paths.imageRoot, "error-1.jpg").apply {
            parentFile?.mkdirs()
            writeText("image")
        }
        val coordinator = coordinator(portable, device, paths)

        coordinator.clearLearningData()

        assertThat(database.errorItemDao().count()).isEqualTo(0)
        assertThat(database.collectionDao().getById("collection-1")).isNull()
        assertThat(paths.imageRoot.listFiles().orEmpty()).isEmpty()
        assertThat(portable.data.first()).isEqualTo(portableValue())
        assertThat(device.data.first()).isEqualTo(deviceValue())
        assertThat(keyStore.read()?.concatToString()).isEqualTo("secret")
    }

    @Test
    fun factoryReset_clearsAllStateAndGeneratesNewDeviceIdentity(): Unit = runBlocking {
        val portable = StoragePreferencesFactory.createPortable(File(root, "slots/slot_a"), scope)
        val device = StoragePreferencesFactory.createDevice(File(root, "device"), scope)
        val paths = paths()
        portable.replace(portableValue())
        device.replace(deviceValue())
        listOf(paths.imageRoot, paths.tempRoot, paths.transferRoot).forEach { directory ->
            File(directory, "data.bin").apply {
                parentFile?.mkdirs()
                writeText("data")
            }
        }
        val coordinator = coordinator(portable, device, paths)

        coordinator.factoryReset()

        assertThat(keyStore.read()).isNull()
        assertThat(portable.data.first()).isEqualTo(PortablePreferences())
        assertThat(device.data.first()).isEqualTo(
            DevicePreferences(
                deviceId = "new-device",
                deviceDisplayName = "伴读题集",
            ),
        )
        assertThat(paths.slotsRoot.exists()).isFalse()
        assertThat(paths.tempRoot.exists()).isFalse()
        assertThat(paths.transferRoot.exists()).isFalse()
    }

    private fun coordinator(
        portable: com.bandu.tiji.core.storage.preferences.PortablePreferencesStore,
        device: com.bandu.tiji.core.storage.preferences.DevicePreferencesStore,
        paths: ResetPaths,
    ) = DefaultLearningDataResetCoordinator(
        database = database,
        portablePreferences = portable,
        devicePreferences = device,
        apiKeyStore = keyStore,
        uuidGenerator = object : UuidGenerator {
            override fun newUuid(): String = "new-device"
        },
        resetPaths = paths,
    )

    private fun paths(): ResetPaths {
        val slots = File(root, "slots")
        return ResetPaths(
            slotsRoot = slots,
            imageRoot = File(slots, "slot_a/images"),
            tempRoot = File(root, "temp"),
            transferRoot = File(root, "transfer"),
        )
    }

    private suspend fun seedLearningData() {
        database.collectionDao().insert(CollectionEntity("collection-1", "题集", 1L, 1L))
        database.errorItemDao().insert(
            ErrorItemEntity(
                id = "error-1",
                collectionId = "collection-1",
                imagePath = "images/error-1.jpg",
                imageSha256 = "abc",
                imageWidth = 100,
                imageHeight = 80,
                questionText = "题目",
                answerText = "答案",
                analysis = "解析",
                mistakeStatus = "UNKNOWN",
                subject = "数学",
                gradeSemester = null,
                paperLevel = null,
                masteryLevel = 0,
                createdAt = 1L,
                updatedAt = 1L,
            ),
        )
    }

    private fun portableValue() =
        PortablePreferences(
            nickname = "小伴",
            educationStage = "junior_high",
            enrollmentYear = 2025,
            providerType = AiProviderType.GEMINI,
            providerDisplayName = "Gemini",
            baseUrl = "https://example.com",
            analysisModel = "analysis",
            tutorModel = "tutor",
            tutorPrompt = "custom",
        )

    private fun deviceValue() =
        DevicePreferences(
            deviceId = "old-device",
            deviceDisplayName = "旧设备",
            trustedPeerRecords = setOf("trusted-peer"),
            identityKeyAlias = "identity",
        )

    private class FakeApiKeyCipherStore : ApiKeyCipherStore {
        private var value: CharArray? = null

        override fun read(): CharArray? = value?.copyOf()

        override fun save(apiKey: CharArray) {
            value = apiKey.copyOf()
        }

        override fun clear() {
            value?.fill('\u0000')
            value = null
        }
    }
}
