package com.bandu.tiji.domain.usecase.transfer

import com.bandu.tiji.domain.util.runSuspendCatching

import com.bandu.tiji.core.common.result.AppResult
import com.bandu.tiji.domain.repository.DeviceTransferRepository
import com.bandu.tiji.domain.transfer.PairingCode

class CreateReceiveCodeUseCase(
    private val repository: DeviceTransferRepository,
) {
    suspend operator fun invoke(): AppResult<PairingCode> =
        runSuspendCatching { repository.createReceiveCode() }
}
