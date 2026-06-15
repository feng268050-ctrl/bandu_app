package com.bandu.tiji.domain.usecase.transfer

import com.bandu.tiji.domain.util.runSuspendCatching

import com.bandu.tiji.core.common.result.AppResult
import com.bandu.tiji.domain.repository.DeviceTransferRepository

class ForgetDeviceUseCase(
    private val repository: DeviceTransferRepository,
) {
    suspend operator fun invoke(deviceId: String): AppResult<Unit> {
        if (deviceId.isBlank()) {
            return AppResult.Failure(
                com.bandu.tiji.core.common.result.AppError.Validation("transfer.device_id.blank"),
            )
        }
        return runSuspendCatching { repository.forgetDevice(deviceId) }
    }
}
