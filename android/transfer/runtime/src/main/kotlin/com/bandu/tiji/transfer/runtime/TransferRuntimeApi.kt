package com.bandu.tiji.transfer.runtime

import com.bandu.tiji.domain.repository.DeviceTransferRepository
import java.io.File
import kotlinx.coroutines.flow.StateFlow

interface TransferRuntimeApi : DeviceTransferRepository {
    val activity: StateFlow<RuntimeActivity>
}

data class TransferRuntimeConfig(
    val filesDirectory: File,
    val serviceType: String = SERVICE_TYPE,
    val connectTimeoutMillis: Long = CONNECT_TIMEOUT_MILLIS,
    val pairingCodeLifetimeMillis: Long = PAIRING_CODE_LIFETIME_MILLIS,
    val resumeLifetimeMillis: Long = RESUME_LIFETIME_MILLIS,
) {
    init {
        require(filesDirectory.path.isNotBlank())
        require(serviceType == SERVICE_TYPE)
        require(connectTimeoutMillis > 0)
        require(pairingCodeLifetimeMillis == PAIRING_CODE_LIFETIME_MILLIS)
        require(resumeLifetimeMillis == RESUME_LIFETIME_MILLIS)
    }

    companion object {
        const val SERVICE_TYPE = "_bandu-tiji._tcp."
        const val CONNECT_TIMEOUT_MILLIS = 10_000L
        const val PAIRING_CODE_LIFETIME_MILLIS = 5 * 60 * 1_000L
        const val RESUME_LIFETIME_MILLIS = 24 * 60 * 60 * 1_000L
        const val CHUNK_SIZE_BYTES = 1024 * 1024
        const val MAX_DEVICE_NAME_BYTES = 32
    }
}

sealed interface RuntimeActivity {
    data object Inactive : RuntimeActivity

    data class Discovery(
        val advertising: Boolean,
        val scanning: Boolean,
    ) : RuntimeActivity

    data class Transfer(
        val sessionId: String,
        val direction: TransferDirection,
    ) : RuntimeActivity
}

enum class TransferDirection {
    SEND,
    RECEIVE,
}
