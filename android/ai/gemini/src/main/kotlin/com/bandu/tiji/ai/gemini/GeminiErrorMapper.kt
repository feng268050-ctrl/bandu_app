package com.bandu.tiji.ai.gemini

import com.bandu.tiji.ai.api.error.AiError
import com.bandu.tiji.ai.api.error.AiErrorMapper
import com.bandu.tiji.core.network.endpoint.EndpointRejectedException

internal object GeminiErrorMapper {
    fun map(error: Throwable): AiError = when (error) {
        is AiError -> error
        is EndpointRejectedException -> AiError.EndpointRejected(
            error.rejectionReason.name.lowercase(),
        )
        is GeminiHttpException -> AiErrorMapper.fromHttpStatus(error.statusCode)
            ?: AiError.InvalidResponse("gemini.http_${error.statusCode}")
        else -> AiErrorMapper.fromThrowable(error)
    }

    fun validationCode(error: Throwable): String = when (map(error)) {
        AiError.Authentication -> "authentication"
        AiError.RateLimited -> "rate_limited"
        AiError.Timeout -> "timeout"
        AiError.NetworkUnavailable -> "network_error"
        is AiError.EndpointRejected -> "endpoint_rejected"
        is AiError.InvalidResponse -> "invalid_response"
        AiError.ConfigurationRequired -> "configuration_required"
        AiError.Cancelled -> "cancelled"
    }
}
