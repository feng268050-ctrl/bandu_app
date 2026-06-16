package com.bandu.tiji.core.storage.slot

import android.content.Context
import com.bandu.tiji.core.storage.db.LearningDatabase
import com.bandu.tiji.core.storage.db.LearningDatabaseFactory
import com.bandu.tiji.core.storage.preferences.DevicePreferencesStore
import java.io.Closeable
import java.io.File
import kotlinx.coroutines.flow.first

class StorageSlotManager(
    private val context: Context,
    private val slotsRoot: File,
    private val devicePreferences: DevicePreferencesStore,
    private val databaseOpener: (Context, File) -> LearningDatabase = LearningDatabaseFactory::open,
) : Closeable {
    private var activeHandle: StorageSlotHandle? = null

    suspend fun openActive(): StorageSlotHandle {
        val slotName = devicePreferences.data.first().activeSlot.requireValidSlotName()
        activeHandle?.let { handle ->
            if (handle.slot.name == slotName && !handle.isClosed) return handle
            handle.close()
        }
        return openSlot(slotName).also { activeHandle = it }
    }

    suspend fun switchActiveSlot(slotName: String): StorageSlotHandle {
        val targetSlot = slotName.requireValidSlotName()
        val preferences = devicePreferences.data.first()
        val previousSlot = preferences.activeSlot.requireValidSlotName()
        if (targetSlot == previousSlot) return openActive()

        activeHandle?.close()
        activeHandle = null

        val next = runCatching {
            openSlot(targetSlot).also { handle ->
                val health = healthCheck(handle)
                check(health.ok) { "Storage slot $targetSlot is not healthy: ${health.integrityCheck}" }
            }
        }.getOrElse { error ->
            activeHandle = openSlot(previousSlot)
            throw error
        }

        devicePreferences.replace(preferences.copy(activeSlot = targetSlot))
        activeHandle = next
        return next
    }

    suspend fun healthCheckActive(): StorageSlotHealth = healthCheck(openActive())

    override fun close() {
        activeHandle?.close()
        activeHandle = null
    }

    fun slot(slotName: String): LearningStorageSlot {
        val safeName = slotName.requireValidSlotName()
        val directory = File(slotsRoot, safeName)
        return LearningStorageSlot(
            name = safeName,
            directory = directory,
            databaseFile = File(directory, DATABASE_FILE_NAME),
            imageDirectory = File(directory, IMAGE_DIRECTORY_NAME),
        )
    }

    private fun openSlot(slotName: String): StorageSlotHandle {
        val slot = slot(slotName)
        require(slot.imageDirectory.exists() || slot.imageDirectory.mkdirs()) {
            "Unable to create image directory for ${slot.name}"
        }
        return StorageSlotHandle(
            slot = slot,
            database = databaseOpener(context, slot.databaseFile),
        )
    }

    private fun healthCheck(handle: StorageSlotHandle): StorageSlotHealth {
        val sqlite = handle.database.openHelper.writableDatabase
        val integrity = sqlite.query("PRAGMA integrity_check").use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else "empty"
        }
        val foreignKeyViolations = sqlite.query("PRAGMA foreign_key_check").use { cursor ->
            cursor.count
        }
        return StorageSlotHealth(
            slotName = handle.slot.name,
            ok = integrity == "ok" && foreignKeyViolations == 0,
            integrityCheck = integrity,
            foreignKeyViolations = foreignKeyViolations,
        )
    }

    companion object {
        const val SLOT_A = "slot_a"
        const val SLOT_B = "slot_b"
        const val DATABASE_FILE_NAME = "learning.db"
        const val IMAGE_DIRECTORY_NAME = "images"
    }
}

data class LearningStorageSlot(
    val name: String,
    val directory: File,
    val databaseFile: File,
    val imageDirectory: File,
)

class StorageSlotHandle(
    val slot: LearningStorageSlot,
    val database: LearningDatabase,
) : Closeable {
    var isClosed: Boolean = false
        private set

    override fun close() {
        if (!isClosed) {
            database.close()
            isClosed = true
        }
    }
}

data class StorageSlotHealth(
    val slotName: String,
    val ok: Boolean,
    val integrityCheck: String,
    val foreignKeyViolations: Int,
)

internal fun String.requireValidSlotName(): String {
    require(this == StorageSlotManager.SLOT_A || this == StorageSlotManager.SLOT_B) {
        "Unsupported storage slot: $this"
    }
    return this
}
