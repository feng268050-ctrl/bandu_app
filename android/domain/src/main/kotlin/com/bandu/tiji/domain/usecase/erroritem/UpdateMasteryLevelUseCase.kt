package com.bandu.tiji.domain.usecase.erroritem

import com.bandu.tiji.core.common.result.AppResult
import com.bandu.tiji.core.model.enums.MasteryLevel
import com.bandu.tiji.core.model.erroritem.ErrorItemPatch
import com.bandu.tiji.core.model.id.ErrorItemId
import com.bandu.tiji.domain.repository.ErrorItemRepository

class UpdateMasteryLevelUseCase(
    private val repository: ErrorItemRepository,
) {
    suspend operator fun invoke(id: ErrorItemId, masteryLevel: MasteryLevel): AppResult<Unit> =
        UpdateErrorItemUseCase(repository).invoke(id, ErrorItemPatch(masteryLevel = masteryLevel))
}
