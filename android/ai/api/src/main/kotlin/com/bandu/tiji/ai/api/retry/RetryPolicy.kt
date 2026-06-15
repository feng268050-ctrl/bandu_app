package com.bandu.tiji.ai.api.retry

import com.bandu.tiji.ai.api.error.AiError

enum class AiOperationType {
    VALIDATE_CONFIGURATION,
    ANALYZE_IMAGE,
    STREAM_TUTOR_FIRST_BYTE,
    GENERATE_EXERCISE,
    GRADE_EXERCISE,
}

data class RetryContext(
    val operation: AiOperationType,
    val attempt: Int,
    val error: AiError,
    val httpStatus: Int? = null,
    val receivedStreamDelta: Boolean = false,
)

sealed interface RetryDecision {
    data object DoNotRetry : RetryDecision

    data class RetryAfter(val delayMillis: Long = 0) : RetryDecision
}

class RetryPolicy(
    private val maxAttempts: Int = 1,
) {
    fun decide(context: RetryContext): RetryDecision {
        if (context.attempt >= maxAttempts) {
            return RetryDecision.DoNotRetry
        }

        if (!isRetriableOperation(context)) {
            return RetryDecision.DoNotRetry
        }

        if (!isRetriableError(context.error, context.httpStatus)) {
            return RetryDecision.DoNotRetry
        }

        return when (context.error) {
            is AiError.RateLimited -> RetryDecision.RetryAfter(delayMillis = 0)
            else -> RetryDecision.RetryAfter(delayMillis = 0)
        }
    }

    private fun isRetriableOperation(context: RetryContext): Boolean = when (context.operation) {
        AiOperationType.VALIDATE_CONFIGURATION -> false
        AiOperationType.STREAM_TUTOR_FIRST_BYTE -> !context.receivedStreamDelta
        AiOperationType.ANALYZE_IMAGE,
        AiOperationType.GENERATE_EXERCISE,
        AiOperationType.GRADE_EXERCISE,
        -> true
    }

    private fun isRetriableError(error: AiError, httpStatus: Int?): Boolean = when (error) {
        AiError.Cancelled,
        AiError.Authentication,
        AiError.ConfigurationRequired,
        is AiError.EndpointRejected,
        is AiError.InvalidResponse,
        -> false
        AiError.RateLimited -> false
        AiError.NetworkUnavailable,
        AiError.Timeout,
        -> true
    }
}
