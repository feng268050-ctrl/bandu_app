package com.bandu.tiji.core.storage.slot

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bandu.tiji.core.storage.db.entity.CollectionEntity
import com.bandu.tiji.core.storage.preferences.DevicePreferences
import com.bandu.tiji.core.storage.preferences.StoragePreferencesFactory
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
class StorageSlotManagerInstrumentedTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val root = File(context.cacheDir, "storage-slot-test-${System.nanoTime()}")
    private val devicePreferences = StoragePreferencesFactory.createDevice(File(root, "device"), scope)
    private val manager = StorageSlotManager(
        context = context,
        slotsRoot = File(root, "slots"),
        devicePreferences = devicePreferences,
    )

    @After
    fun tearDown() {
        manager.close()
        scope.cancel()
        root.deleteRecursively()
    }

    @Test
    fun openActiveCreatesSlotAAndReportsHealthyDatabase() = runBlocking {
        devicePreferences.replace(DevicePreferences(activeSlot = StorageSlotManager.SLOT_A))

        val handle = manager.openActive()
        val health = manager.healthCheckActive()

        assertThat(handle.slot.name).isEqualTo(StorageSlotManager.SLOT_A)
        assertThat(handle.isClosed).isFalse()
        assertThat(health.ok).isTrue()
        assertThat(health.integrityCheck).isEqualTo("ok")
        assertThat(health.foreignKeyViolations).isEqualTo(0)
        assertThat(File(root, "slots/slot_a/${StorageSlotManager.DATABASE_FILE_NAME}").exists())
            .isTrue()
        assertThat(File(root, "slots/slot_a/${StorageSlotManager.IMAGE_DIRECTORY_NAME}").isDirectory)
            .isTrue()
    }

    @Test
    fun switchActiveSlotClosesOldDatabaseAndKeepsSlotsIndependent() = runBlocking {
        devicePreferences.replace(DevicePreferences(activeSlot = StorageSlotManager.SLOT_A))
        val slotA = manager.openActive()
        slotA.database.collectionDao().insert(collection("collection-a", "代数"))

        val slotB = manager.switchActiveSlot(StorageSlotManager.SLOT_B)

        assertThat(slotA.isClosed).isTrue()
        assertThat(slotB.slot.name).isEqualTo(StorageSlotManager.SLOT_B)
        assertThat(devicePreferences.data.first().activeSlot).isEqualTo(StorageSlotManager.SLOT_B)
        assertThat(slotB.database.collectionDao().getById("collection-a")).isNull()
        slotB.database.collectionDao().insert(collection("collection-b", "几何"))

        val reopenedSlotA = manager.switchActiveSlot(StorageSlotManager.SLOT_A)

        assertThat(slotB.isClosed).isTrue()
        assertThat(reopenedSlotA.database.collectionDao().getById("collection-a")).isNotNull()
        assertThat(reopenedSlotA.database.collectionDao().getById("collection-b")).isNull()
        assertThat(devicePreferences.data.first().activeSlot).isEqualTo(StorageSlotManager.SLOT_A)
    }

    private fun collection(
        id: String,
        name: String,
    ) = CollectionEntity(
        id = id,
        name = name,
        createdAt = 100,
        updatedAt = 100,
    )
}
