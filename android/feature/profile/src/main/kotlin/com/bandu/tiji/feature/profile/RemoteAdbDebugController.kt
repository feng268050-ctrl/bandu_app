package com.bandu.tiji.feature.profile

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

const val DEFAULT_REMOTE_ADB_PORT = 5555

data class RemoteAdbDebugRuntimeState(
    val isSupported: Boolean = false,
    val isEnabled: Boolean = false,
    val port: Int = DEFAULT_REMOTE_ADB_PORT,
    val ipAddress: String? = null,
    val unsupportedReason: String? = null,
)

sealed interface RemoteAdbDebugOperationResult {
    data class Success(
        val state: RemoteAdbDebugRuntimeState,
    ) : RemoteAdbDebugOperationResult

    data class Unsupported(
        val reason: String,
        val state: RemoteAdbDebugRuntimeState,
    ) : RemoteAdbDebugOperationResult

    data class Failure(
        val message: String,
        val state: RemoteAdbDebugRuntimeState,
    ) : RemoteAdbDebugOperationResult
}

interface RemoteAdbDebugController {
    fun observeState(): Flow<RemoteAdbDebugRuntimeState>

    suspend fun refresh(): RemoteAdbDebugRuntimeState

    suspend fun enable(port: Int = DEFAULT_REMOTE_ADB_PORT): RemoteAdbDebugOperationResult

    suspend fun disable(): RemoteAdbDebugOperationResult
}

object NoOpRemoteAdbDebugController : RemoteAdbDebugController {
    private val state = MutableStateFlow(
        RemoteAdbDebugRuntimeState(
            isSupported = false,
            unsupportedReason = "当前安装形态不支持系统级远程调试",
        ),
    )

    override fun observeState(): Flow<RemoteAdbDebugRuntimeState> = state

    override suspend fun refresh(): RemoteAdbDebugRuntimeState = state.value

    override suspend fun enable(port: Int): RemoteAdbDebugOperationResult =
        RemoteAdbDebugOperationResult.Unsupported(
            reason = requireNotNull(state.value.unsupportedReason),
            state = state.value,
        )

    override suspend fun disable(): RemoteAdbDebugOperationResult =
        RemoteAdbDebugOperationResult.Unsupported(
            reason = requireNotNull(state.value.unsupportedReason),
            state = state.value,
        )
}
