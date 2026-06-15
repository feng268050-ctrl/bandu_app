package com.bandu.tiji.domain.usecase.transfer

import com.bandu.tiji.core.common.result.AppError
import com.bandu.tiji.core.common.result.AppResult
import com.bandu.tiji.domain.repository.DeviceTransferRepository
import com.bandu.tiji.domain.transfer.NearbyDevice
import com.bandu.tiji.domain.transfer.PairingResult

class PairDeviceUseCase(
    private val repository: DeviceTransferRepository,
) {
    suspend operator fun invoke(device: NearbyDevice, code: String): AppResult<Unit> {
        if (!CODE_REGEX.matches(code)) {
            return AppResult.Failure(AppError.Validation("transfer.pairing_code.invalid"))
        }
        return when (val result = repository.pair(device, code)) {
            PairingResult.Success -> AppResult.Success(Unit)
            is PairingResult.Failure -> AppResult.Failure(AppError.Transfer(mapFailure(result.code)))
        }
    }

    private fun mapFailure(code: com.bandu.tiji.domain.transfer.TransferFailureCode) =
        when (code) {
            com.bandu.tiji.domain.transfer.TransferFailureCode.PAIRING_EXPIRED,
            com.bandu.tiji.domain.transfer.TransferFailureCode.PAIRING_FAILED,
            -> com.bandu.tiji.core.common.result.TransferKind.PROTOCOL_ERROR
            com.bandu.tiji.domain.transfer.TransferFailureCode.CHECKSUM_FAILED ->
                com.bandu.tiji.core.common.result.TransferKind.CHECKSUM_FAILED
            com.bandu.tiji.domain.transfer.TransferFailureCode.NETWORK_INTERRUPTED ->
                com.bandu.tiji.core.common.result.TransferKind.INTERRUPTED
            else -> com.bandu.tiji.core.common.result.TransferKind.PROTOCOL_ERROR
        }

    companion object {
        private val CODE_REGEX = Regex("""\d{6}""")
    }
}
