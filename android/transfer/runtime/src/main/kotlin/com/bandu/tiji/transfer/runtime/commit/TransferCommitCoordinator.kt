package com.bandu.tiji.transfer.runtime.commit

import com.bandu.tiji.core.storage.keystore.ApiKeyStore
import com.bandu.tiji.core.storage.preferences.DevicePreferencesStore
import com.bandu.tiji.core.storage.slot.PendingCommitRecoveryResult
import com.bandu.tiji.core.storage.slot.StorageSlotManager
import kotlinx.coroutines.flow.first

class TransferCommitCoordinator(
    private val gateway: TransferCommitGateway,
) {
    suspend fun inactiveSlot(): String = gateway.activeSlot().otherSlot()

    suspend fun commitVerifiedImport(
        sessionId: String,
        inactiveSlot: String? = null,
    ): CommitResult {
        require(sessionId.isNotBlank()) { "Session ID must not be blank" }
        val targetSlot = inactiveSlot ?: inactiveSlot()
        return try {
            gateway.verifySlot(targetSlot)
            gateway.markPendingCommit(targetSlot)
            gateway.switchActiveSlot(targetSlot)
            gateway.clearApiKey()
            when (val recovery = gateway.finalizePendingCommit()) {
                is CommitRecovery.Finalized -> CommitResult.Committed(recovery.activeSlot)
                is CommitRecovery.RolledBack -> CommitResult.RolledBack(recovery.reason)
                is CommitRecovery.Failed -> CommitResult.Failed(recovery.reason)
                CommitRecovery.NoPending -> CommitResult.Failed("pending commit was not recorded")
            }
        } catch (error: Exception) {
            CommitResult.Failed(error.message.orEmpty())
        }
    }
}

interface TransferCommitGateway {
    suspend fun activeSlot(): String

    suspend fun verifySlot(slotName: String)

    suspend fun markPendingCommit(slotName: String)

    suspend fun switchActiveSlot(slotName: String)

    fun clearApiKey()

    suspend fun finalizePendingCommit(): CommitRecovery
}

class StorageSlotTransferCommitGateway(
    private val slotManager: StorageSlotManager,
    private val devicePreferences: DevicePreferencesStore,
    private val apiKeyStore: ApiKeyStore,
) : TransferCommitGateway {
    override suspend fun activeSlot(): String =
        devicePreferences.data.first().activeSlot

    override suspend fun verifySlot(slotName: String) {
        slotManager.switchActiveSlot(slotName).close()
        slotManager.switchActiveSlot(slotName.otherSlot()).close()
    }

    override suspend fun markPendingCommit(slotName: String) {
        slotManager.markPendingCommit(slotName)
    }

    override suspend fun switchActiveSlot(slotName: String) {
        slotManager.switchActiveSlot(slotName)
    }

    override fun clearApiKey() {
        apiKeyStore.clear()
    }

    override suspend fun finalizePendingCommit(): CommitRecovery =
        when (val result = slotManager.recoverPendingCommit()) {
            PendingCommitRecoveryResult.NoPending -> CommitRecovery.NoPending
            is PendingCommitRecoveryResult.Finalized ->
                CommitRecovery.Finalized(result.activeSlot)
            is PendingCommitRecoveryResult.RolledBack ->
                CommitRecovery.RolledBack(result.reason)
            is PendingCommitRecoveryResult.Failed ->
                CommitRecovery.Failed(result.reason)
        }
}

sealed interface CommitResult {
    data class Committed(val activeSlot: String) : CommitResult

    data class RolledBack(val reason: String) : CommitResult

    data class Failed(val reason: String) : CommitResult
}

sealed interface CommitRecovery {
    data object NoPending : CommitRecovery

    data class Finalized(val activeSlot: String) : CommitRecovery

    data class RolledBack(val reason: String) : CommitRecovery

    data class Failed(val reason: String) : CommitRecovery
}

internal fun String.otherSlot(): String = when (this) {
    StorageSlotManager.SLOT_A -> StorageSlotManager.SLOT_B
    StorageSlotManager.SLOT_B -> StorageSlotManager.SLOT_A
    else -> throw IllegalArgumentException("Unsupported storage slot: $this")
}
