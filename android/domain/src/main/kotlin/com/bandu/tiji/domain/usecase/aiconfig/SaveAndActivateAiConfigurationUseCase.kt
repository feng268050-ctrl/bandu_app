package com.bandu.tiji.domain.usecase.aiconfig

import com.bandu.tiji.core.common.result.AppError
import com.bandu.tiji.core.common.result.AppResult
import com.bandu.tiji.domain.ai.AiConfigurationDraft
import com.bandu.tiji.domain.ai.ValidationResult
import com.bandu.tiji.domain.repository.AiConfigurationRepository

class SaveAndActivateAiConfigurationUseCase(
    private val repository: AiConfigurationRepository,
) {
    suspend operator fun invoke(draft: AiConfigurationDraft): AppResult<Unit> {
        if (draft.displayName.isBlank()) {
            return AppResult.Failure(AppError.Validation("ai.display_name.blank"))
        }
        if (draft.baseUrl.isBlank()) {
            return AppResult.Failure(AppError.Validation("ai.base_url.blank"))
        }
        if (draft.analysisModel.isBlank() || draft.tutorModel.isBlank()) {
            return AppResult.Failure(AppError.Validation("ai.model.blank"))
        }

        return when (val result = repository.saveAndActivate(draft)) {
            ValidationResult.Success -> AppResult.Success(Unit)
            is ValidationResult.Failure -> AppResult.Failure(
                AppError.Validation(result.errors.joinToString(";")),
            )
        }
    }
}
