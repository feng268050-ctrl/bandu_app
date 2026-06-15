package com.bandu.tiji.domain.usecase.collection

import com.bandu.tiji.domain.util.runSuspendCatching

import com.bandu.tiji.core.common.result.AppResult
import com.bandu.tiji.core.model.id.CollectionId
import com.bandu.tiji.domain.repository.CollectionRepository
import com.bandu.tiji.domain.validation.CollectionNameValidator

class RenameCollectionUseCase(
    private val repository: CollectionRepository,
    private val nameValidator: CollectionNameValidator = CollectionNameValidator(),
) {
    suspend operator fun invoke(id: CollectionId, name: String): AppResult<Unit> =
        when (val validated = nameValidator.validate(name)) {
            is AppResult.Success -> runSuspendCatching { repository.rename(id, validated.value) }
            is AppResult.Failure -> validated
        }
}
