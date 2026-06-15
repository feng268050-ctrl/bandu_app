package com.bandu.tiji.ai.api.error

sealed class AiError : Exception() {
    data object ConfigurationRequired : AiError()

    data object Authentication : AiError()

    data object RateLimited : AiError()

    data object Timeout : AiError()

    data object NetworkUnavailable : AiError()

    data class InvalidResponse(val diagnosticCode: String) : AiError()

    data class EndpointRejected(val reason: String) : AiError()

    data object Cancelled : AiError()
}

object AiErrorMapper {
    fun fromHttpStatus(status: Int): AiError? = when (status) {
        401, 403 -> AiError.Authentication
        408, 504 -> AiError.Timeout
        429 -> AiError.RateLimited
        in 500..599 -> AiError.NetworkUnavailable
        else -> null
    }

    fun fromThrowable(throwable: Throwable): AiError = when (throwable) {
        is AiError -> throwable
        else -> when (throwable) {
            is java.util.concurrent.CancellationException,
            is kotlinx.coroutines.CancellationException,
            -> AiError.Cancelled
            is java.net.SocketTimeoutException,
            is java.io.InterruptedIOException,
            -> AiError.Timeout
            is java.io.IOException,
            is java.net.UnknownHostException,
            -> AiError.NetworkUnavailable
            else -> AiError.InvalidResponse("unexpected:${throwable::class.simpleName}")
        }
    }
}
