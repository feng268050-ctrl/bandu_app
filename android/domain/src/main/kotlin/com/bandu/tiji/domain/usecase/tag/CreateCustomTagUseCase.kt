package com.bandu.tiji.domain.usecase.tag

import com.bandu.tiji.domain.util.runSuspendCatching

import com.bandu.tiji.core.common.result.AppError
import com.bandu.tiji.core.common.result.AppResult
import com.bandu.tiji.core.model.id.TagId
import com.bandu.tiji.domain.repository.TagRepository
import com.bandu.tiji.domain.tag.CreateTagInput

class CreateCustomTagUseCase(
    private val repository: TagRepository,
) {
    suspend operator fun invoke(input: CreateTagInput): AppResult<TagId> {
        val trimmedName = input.name.trim()
        if (trimmedName.isEmpty()) {
            return AppResult.Failure(AppError.Validation("tag.name.blank"))
        }
        if (input.subject.isBlank()) {
            return AppResult.Failure(AppError.Validation("tag.subject.blank"))
        }
        return runSuspendCatching {
            repository.createCustom(input.copy(name = trimmedName))
        }
    }
}
