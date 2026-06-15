package com.bandu.tiji.ai.openai

import com.bandu.tiji.ai.api.error.AiError
import com.bandu.tiji.ai.api.error.AiErrorMapper
import com.bandu.tiji.core.network.endpoint.EndpointRejectedException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal object OpenAiErrorMapper {
    fun map(error: Throwable): AiError = when (error) {
        is AiError -> error
        is EndpointRejectedException -> AiError.EndpointRejected(
            error.rejectionReason.name.lowercase(),
        )
        is OpenAiHttpException -> mapHttpError(error)
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

    private fun mapHttpError(error: OpenAiHttpException): AiError {
        AiErrorMapper.fromHttpStatus(error.statusCode)?.let { return it }
        val descriptors = parseDescriptors(error.responseBody)
        return when {
            descriptors.any { descriptor -> descriptor.containsAny(AUTH_MARKERS) } ->
                AiError.Authentication
            descriptors.any { descriptor -> descriptor.containsAny(RATE_LIMIT_MARKERS) } ->
                AiError.RateLimited
            descriptors.any { descriptor -> descriptor.containsAny(TIMEOUT_MARKERS) } ->
                AiError.Timeout
            else -> AiError.InvalidResponse("openai.http_${error.statusCode}")
        }
    }

    private fun parseDescriptors(body: String?): List<String> {
        if (body.isNullOrBlank()) return emptyList()
        return runCatching {
            val root = JSON.parseToJsonElement(body).jsonObject
            val error = root["error"]
            buildList {
                addDescriptor(error)
                if (error != null) {
                    runCatching {
                        val errorObject = error.jsonObject
                        addDescriptor(errorObject["code"])
                        addDescriptor(errorObject["type"])
                        addDescriptor(errorObject["message"])
                    }
                }
                addDescriptor(root["code"])
                addDescriptor(root["type"])
                addDescriptor(root["message"])
            }
        }.getOrDefault(emptyList())
    }

    private fun MutableList<String>.addDescriptor(element: JsonElement?) {
        val descriptor = runCatching {
            element?.jsonPrimitive?.contentOrNull
        }.getOrNull()
        if (!descriptor.isNullOrBlank()) {
            add(descriptor.lowercase())
        }
    }

    private fun String.containsAny(markers: Set<String>): Boolean =
        markers.any(::contains)

    private val AUTH_MARKERS = setOf(
        "invalid_api_key",
        "authentication",
        "unauthorized",
        "permission_denied",
    )
    private val RATE_LIMIT_MARKERS = setOf(
        "rate_limit",
        "rate limit",
        "quota_exceeded",
    )
    private val TIMEOUT_MARKERS = setOf(
        "timeout",
        "timed out",
    )
    private val JSON = Json {
        ignoreUnknownKeys = true
    }
}
