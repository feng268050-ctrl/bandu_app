package com.bandu.tiji.domain.usecase.erroritem

import com.bandu.tiji.core.common.result.AppError
import com.bandu.tiji.core.common.result.AppResult
import com.bandu.tiji.core.model.erroritem.ErrorItemDraft
import com.bandu.tiji.core.model.id.ErrorItemId
import com.bandu.tiji.domain.repository.ErrorItemRepository

class CreateErrorItemUseCase(
    private val repository: ErrorItemRepository,
) {
    suspend operator fun invoke(draft: ErrorItemDraft): AppResult<ErrorItemId> =
        try {
            AppResult.Success(repository.create(draft))
        } catch (error: IllegalArgumentException) {
            AppResult.Failure(AppError.Validation(error.message ?: "error_item.invalid"))
        } catch (error: Throwable) {
            AppResult.Failure(AppError.Unexpected(error))
        }
}
