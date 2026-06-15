package com.bandu.tiji.domain.usecase.transfer

import com.bandu.tiji.domain.util.runSuspendCatching

import com.bandu.tiji.core.common.result.AppResult
import com.bandu.tiji.domain.repository.DeviceTransferRepository
import com.bandu.tiji.domain.transfer.TrustedDevice

class SendAllDataUseCase(
    private val repository: DeviceTransferRepository,
) {
    suspend operator fun invoke(target: TrustedDevice): AppResult<Unit> =
        runSuspendCatching { repository.sendAll(target) }
}
