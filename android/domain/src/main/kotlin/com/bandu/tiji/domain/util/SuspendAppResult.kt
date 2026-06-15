package com.bandu.tiji.domain.util

import com.bandu.tiji.core.common.result.AppError
import com.bandu.tiji.core.common.result.AppResult

suspend inline fun <T> runSuspendCatching(crossinline block: suspend () -> T): AppResult<T> =
    try {
        AppResult.Success(block())
    } catch (error: Throwable) {
        AppResult.Failure(AppError.Unexpected(error))
    }
