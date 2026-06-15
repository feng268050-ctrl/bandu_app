package com.bandu.tiji.domain.validation

import com.bandu.tiji.core.common.result.AppError
import com.bandu.tiji.core.common.result.AppResult

class CollectionNameValidator {
    fun validate(name: String): AppResult<String> {
        val trimmed = name.trim()
        return when {
            trimmed.isEmpty() -> AppResult.Failure(AppError.Validation(CODE_BLANK))
            trimmed.length > MAX_LENGTH -> AppResult.Failure(AppError.Validation(CODE_TOO_LONG))
            else -> AppResult.Success(trimmed)
        }
    }

    companion object {
        const val MAX_LENGTH = 64
        const val CODE_BLANK = "collection.name.blank"
        const val CODE_TOO_LONG = "collection.name.too_long"
    }
}
