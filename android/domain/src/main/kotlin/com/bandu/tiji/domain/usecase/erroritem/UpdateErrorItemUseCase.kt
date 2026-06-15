package com.bandu.tiji.domain.usecase.erroritem

import com.bandu.tiji.core.common.result.AppError
import com.bandu.tiji.core.common.result.AppResult
import com.bandu.tiji.core.model.erroritem.ErrorItemPatch
import com.bandu.tiji.core.model.id.ErrorItemId
import com.bandu.tiji.domain.repository.ErrorItemRepository

class UpdateErrorItemUseCase(
    private val repository: ErrorItemRepository,
) {
    suspend operator fun invoke(id: ErrorItemId, patch: ErrorItemPatch): AppResult<Unit> =
        try {
            AppResult.Success(repository.update(id, patch))
        } catch (error: IllegalArgumentException) {
            AppResult.Failure(AppError.Validation(error.message ?: "error_item.invalid"))
        } catch (error: Throwable) {
            AppResult.Failure(AppError.Unexpected(error))
        }
}
