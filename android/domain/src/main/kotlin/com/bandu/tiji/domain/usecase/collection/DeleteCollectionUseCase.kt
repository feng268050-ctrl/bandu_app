package com.bandu.tiji.domain.usecase.collection

import com.bandu.tiji.domain.util.runSuspendCatching

import com.bandu.tiji.core.common.result.AppResult
import com.bandu.tiji.core.model.id.CollectionId
import com.bandu.tiji.domain.repository.CollectionRepository

class DeleteCollectionUseCase(
    private val repository: CollectionRepository,
) {
    suspend operator fun invoke(id: CollectionId): AppResult<Unit> =
        runSuspendCatching { repository.delete(id) }
}
