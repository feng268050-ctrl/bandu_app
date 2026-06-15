package com.bandu.tiji.domain.usecase.transfer

import com.bandu.tiji.domain.util.runSuspendCatching

import com.bandu.tiji.core.common.result.AppResult
import com.bandu.tiji.domain.repository.DeviceTransferRepository

class RejectTransferUseCase(
    private val repository: DeviceTransferRepository,
) {
    suspend operator fun invoke(sessionId: String): AppResult<Unit> =
        runSuspendCatching { repository.rejectTransfer(sessionId) }
}
