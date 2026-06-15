package com.bandu.tiji.domain.usecase.tag

import com.bandu.tiji.domain.util.runSuspendCatching

import com.bandu.tiji.core.common.result.AppError
import com.bandu.tiji.core.common.result.AppResult
import com.bandu.tiji.core.model.id.TagId
import com.bandu.tiji.domain.repository.TagRepository

class DeleteCustomTagUseCase(
    private val repository: TagRepository,
) {
    suspend operator fun invoke(id: TagId): AppResult<Unit> {
        val tag = repository.findTag(id)
            ?: return AppResult.Failure(AppError.NotFound("tag"))
        if (tag.isSystem) {
            return AppResult.Failure(AppError.Validation("tag.system_readonly"))
        }
        return runSuspendCatching { repository.deleteCustom(id) }
    }
}
