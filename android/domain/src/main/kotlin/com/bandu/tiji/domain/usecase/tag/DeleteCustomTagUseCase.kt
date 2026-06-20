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
        return runSuspendCatching {
            val tag = repository.findTag(id)
                ?: throw MissingTagException
            if (tag.isSystem) throw SystemTagReadOnlyException
            repository.deleteCustom(id)
        }.mapTagValidationErrors()
    }

    private fun AppResult<Unit>.mapTagValidationErrors(): AppResult<Unit> =
        when (this) {
            is AppResult.Success -> this
            is AppResult.Failure -> when ((error as? AppError.Unexpected)?.cause) {
                MissingTagException -> AppResult.Failure(AppError.NotFound("tag"))
                SystemTagReadOnlyException ->
                    AppResult.Failure(AppError.Validation("tag.system_readonly"))
                else -> this
            }
        }

    private data object MissingTagException : IllegalStateException()

    private data object SystemTagReadOnlyException : IllegalStateException()
}
