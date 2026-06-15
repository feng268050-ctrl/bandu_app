package com.bandu.tiji.domain.usecase.tag

import com.bandu.tiji.domain.util.runSuspendCatching

import com.bandu.tiji.core.common.result.AppError
import com.bandu.tiji.core.common.result.AppResult
import com.bandu.tiji.core.model.id.TagId
import com.bandu.tiji.domain.repository.TagRepository

class RenameCustomTagUseCase(
    private val repository: TagRepository,
) {
    suspend operator fun invoke(id: TagId, name: String): AppResult<Unit> {
        val tag = repository.findTag(id)
            ?: return AppResult.Failure(AppError.NotFound("tag"))
        if (tag.isSystem) {
            return AppResult.Failure(AppError.Validation("tag.system_readonly"))
        }
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) {
            return AppResult.Failure(AppError.Validation("tag.name.blank"))
        }
        return runSuspendCatching { repository.renameCustom(id, trimmedName) }
    }
}
