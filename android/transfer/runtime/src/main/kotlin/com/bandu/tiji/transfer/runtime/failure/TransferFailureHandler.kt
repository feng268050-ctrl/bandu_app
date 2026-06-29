package com.bandu.tiji.transfer.runtime.failure

import com.bandu.tiji.domain.transfer.TransferFailureCode
import com.bandu.tiji.domain.transfer.TransferState
import java.io.File

class TransferFailureHandler(
    private val transferRoot: File,
) {
    fun handle(
        sessionId: String,
        cause: RuntimeFailureCause,
    ): TransferFailureDecision {
        require(sessionId.isNotBlank()) { "Session ID must not be blank" }
        val decision = cause.toDecision()
        when (decision.cleanup) {
            CleanupMode.DELETE_SESSION -> File(transferRoot, sessionId).deleteRecursively()
            CleanupMode.PRESERVE_RESUME,
            CleanupMode.PRESERVE_TARGET_DATA,
            -> Unit
        }
        return decision
    }
}

enum class RuntimeFailureCause {
    USER_CANCELLED,
    NETWORK_INTERRUPTED,
    INTEGRITY_CHECK_FAILED,
    PROTOCOL_ERROR,
    SPACE_UNAVAILABLE,
    COMMIT_FAILED,
}

data class TransferFailureDecision(
    val state: TransferState.Failed,
    val cleanup: CleanupMode,
)

enum class CleanupMode {
    PRESERVE_RESUME,
    PRESERVE_TARGET_DATA,
    DELETE_SESSION,
}

private fun RuntimeFailureCause.toDecision(): TransferFailureDecision =
    when (this) {
        RuntimeFailureCause.USER_CANCELLED ->
            TransferFailureDecision(
                state = TransferState.Failed(TransferFailureCode.CANCELLED, resumable = false),
                cleanup = CleanupMode.DELETE_SESSION,
            )
        RuntimeFailureCause.NETWORK_INTERRUPTED ->
            TransferFailureDecision(
                state = TransferState.Failed(TransferFailureCode.NETWORK_INTERRUPTED, resumable = true),
                cleanup = CleanupMode.PRESERVE_RESUME,
            )
        RuntimeFailureCause.INTEGRITY_CHECK_FAILED ->
            TransferFailureDecision(
                state = TransferState.Failed(TransferFailureCode.CHECKSUM_FAILED, resumable = false),
                cleanup = CleanupMode.DELETE_SESSION,
            )
        RuntimeFailureCause.PROTOCOL_ERROR ->
            TransferFailureDecision(
                state = TransferState.Failed(TransferFailureCode.PROTOCOL_ERROR, resumable = false),
                cleanup = CleanupMode.DELETE_SESSION,
            )
        RuntimeFailureCause.SPACE_UNAVAILABLE ->
            TransferFailureDecision(
                state = TransferState.Failed(TransferFailureCode.NETWORK_INTERRUPTED, resumable = true),
                cleanup = CleanupMode.PRESERVE_RESUME,
            )
        RuntimeFailureCause.COMMIT_FAILED ->
            TransferFailureDecision(
                state = TransferState.Failed(TransferFailureCode.COMMIT_FAILED, resumable = false),
                cleanup = CleanupMode.PRESERVE_TARGET_DATA,
            )
    }
