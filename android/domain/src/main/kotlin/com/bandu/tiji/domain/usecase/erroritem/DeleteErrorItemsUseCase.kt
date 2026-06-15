package com.bandu.tiji.domain.usecase.erroritem

import com.bandu.tiji.domain.util.runSuspendCatching

import com.bandu.tiji.core.common.result.AppError
import com.bandu.tiji.core.common.result.AppResult
import com.bandu.tiji.core.model.id.ErrorItemId
import com.bandu.tiji.domain.repository.ErrorItemRepository

class DeleteErrorItemsUseCase(
    private val repository: ErrorItemRepository,
) {
    suspend operator fun invoke(ids: Set<ErrorItemId>): AppResult<Unit> {
        if (ids.isEmpty()) {
            return AppResult.Failure(AppError.Validation("error_item.delete.empty"))
        }
        return runSuspendCatching { repository.delete(ids) }
    }
}
