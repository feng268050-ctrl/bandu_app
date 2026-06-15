package com.bandu.tiji.ai.openai

import com.bandu.tiji.ai.api.config.ProviderValidation
import com.bandu.tiji.ai.api.config.ResolvedAiConfiguration
import com.bandu.tiji.ai.api.model.AiStreamEvent
import com.bandu.tiji.ai.api.model.AnalyzeImageRequest
import com.bandu.tiji.ai.api.model.AnalyzedQuestion
import com.bandu.tiji.ai.api.model.ExerciseGrade
import com.bandu.tiji.ai.api.model.ExerciseRequest
import com.bandu.tiji.ai.api.model.GeneratedExercise
import com.bandu.tiji.ai.api.model.GradeExerciseRequest
import com.bandu.tiji.ai.api.model.TutorRequest
import com.bandu.tiji.ai.api.provider.AiProvider
import com.bandu.tiji.core.model.enums.AiProviderType
import com.bandu.tiji.core.network.AiHttpOperation
import com.bandu.tiji.core.network.HttpEngine
import com.bandu.tiji.core.network.sse.SseEvent
import com.bandu.tiji.core.network.sse.SseReader
import java.io.IOException
import java.util.Base64
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

fun interface OpenAiHttpEngineFactory {
    fun create(
        configuration: ResolvedAiConfiguration,
        operation: AiHttpOperation,
    ): HttpEngine
}

class OpenAiCompatibleProvider(
    private val httpEngineFactory: OpenAiHttpEngineFactory = OpenAiHttpEngineFactory { _, _ ->
        error("OpenAiHttpEngineFactory is not configured")
    },
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : AiProvider {
    override val type: AiProviderType = AiProviderType.OPENAI_COMPATIBLE

    override suspend fun validate(
        configuration: ResolvedAiConfiguration,
    ): ProviderValidation = withContext(ioDispatcher) {
        try {
            val request = Request.Builder()
                .url(configuration.chatCompletionsEndpoint())
                .header(AUTHORIZATION_HEADER, "Bearer ${configuration.apiKey}")
                .post(validationBody(configuration.analysisModel).toRequestBody(JSON_MEDIA_TYPE))
                .build()
            httpEngineFactory
                .create(configuration, AiHttpOperation.CONFIGURATION_VALIDATION)
                .newCall(request)
                .execute()
                .use { response ->
                    if (!response.isSuccessful) {
                        throw OpenAiHttpException(response.code)
                    }
                    ProviderValidation.Success
                }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            ProviderValidation.Failure(validationFailureCode(error))
        }
    }

    override suspend fun analyzeImage(
        configuration: ResolvedAiConfiguration,
        request: AnalyzeImageRequest,
    ): AnalyzedQuestion = unsupported()

    override fun streamTutor(
        configuration: ResolvedAiConfiguration,
        request: TutorRequest,
    ): Flow<AiStreamEvent> = unsupported()

    override suspend fun generateExercise(
        configuration: ResolvedAiConfiguration,
        request: ExerciseRequest,
    ): GeneratedExercise = unsupported()

    override suspend fun gradeExercise(
        configuration: ResolvedAiConfiguration,
        request: GradeExerciseRequest,
    ): ExerciseGrade = unsupported()

    internal suspend fun generateText(
        configuration: ResolvedAiConfiguration,
        model: String,
        prompt: String,
        operation: AiHttpOperation,
    ): String = executeChatCompletion(
        configuration = configuration,
        operation = operation,
        body = textRequestBody(model, prompt),
    )

    internal suspend fun generateImageAnalysisText(
        configuration: ResolvedAiConfiguration,
        request: AnalyzeImageRequest,
        prompt: String,
    ): String = executeChatCompletion(
        configuration = configuration,
        operation = AiHttpOperation.IMAGE_ANALYSIS,
        body = imageRequestBody(
            model = configuration.analysisModel,
            request = request,
            prompt = prompt,
        ),
    )

    internal fun streamText(
        configuration: ResolvedAiConfiguration,
        model: String,
        prompt: String,
    ): Flow<AiStreamEvent> = callbackFlow {
        val request = Request.Builder()
            .url(configuration.chatCompletionsEndpoint())
            .header(AUTHORIZATION_HEADER, "Bearer ${configuration.apiKey}")
            .post(streamRequestBody(model, prompt).toRequestBody(JSON_MEDIA_TYPE))
            .build()
        val call = httpEngineFactory
            .create(configuration, AiHttpOperation.TUTOR_STREAM)
            .newCall(request)
        call.enqueue(
            object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (!call.isCanceled()) close(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    launch(ioDispatcher) {
                        try {
                            response.use {
                                if (!response.isSuccessful) {
                                    throw OpenAiHttpException(response.code)
                                }
                                SseReader().readEach(response.body.source()) { event ->
                                    when (event) {
                                        is SseEvent.Data -> {
                                            extractStreamDelta(event.data)?.let { delta ->
                                                send(AiStreamEvent.Delta(delta))
                                            }
                                        }
                                        SseEvent.Done -> Unit
                                    }
                                }
                            }
                            send(AiStreamEvent.Completed)
                            close()
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (error: Throwable) {
                            close(error)
                        }
                    }
                }
            },
        )
        awaitClose { call.cancel() }
    }

    private suspend fun executeChatCompletion(
        configuration: ResolvedAiConfiguration,
        operation: AiHttpOperation,
        body: String,
    ): String = withContext(ioDispatcher) {
        val request = Request.Builder()
            .url(configuration.chatCompletionsEndpoint())
            .header(AUTHORIZATION_HEADER, "Bearer ${configuration.apiKey}")
            .post(body.toRequestBody(JSON_MEDIA_TYPE))
            .build()
        httpEngineFactory.create(configuration, operation)
            .newCall(request)
            .execute()
            .use { response ->
                if (!response.isSuccessful) {
                    throw OpenAiHttpException(response.code)
                }
                extractCompletionText(response.body.string())
            }
    }

    private fun <T> unsupported(): T =
        throw UnsupportedOperationException("OpenAI-compatible operation is not implemented")

    private fun validationBody(model: String): String = buildJsonObject {
        put("model", model)
        putJsonArray("messages") {
            add(
                buildJsonObject {
                    put("role", "user")
                    put("content", "ping")
                },
            )
        }
        put("max_tokens", 1)
        put("stream", false)
    }.toString()

    private fun textRequestBody(model: String, prompt: String): String = buildJsonObject {
        put("model", model)
        putJsonArray("messages") {
            add(
                buildJsonObject {
                    put("role", "user")
                    put("content", prompt)
                },
            )
        }
        put("stream", false)
    }.toString()

    private fun imageRequestBody(
        model: String,
        request: AnalyzeImageRequest,
        prompt: String,
    ): String = buildJsonObject {
        put("model", model)
        putJsonArray("messages") {
            add(
                buildJsonObject {
                    put("role", "user")
                    putJsonArray("content") {
                        add(
                            buildJsonObject {
                                put("type", "text")
                                put("text", prompt)
                            },
                        )
                        add(
                            buildJsonObject {
                                put("type", "image_url")
                                put(
                                    "image_url",
                                    buildJsonObject {
                                        put(
                                            "url",
                                            "data:${request.mimeType};base64," +
                                                Base64.getEncoder()
                                                    .encodeToString(request.imageBytes),
                                        )
                                    },
                                )
                            },
                        )
                    }
                },
            )
        }
        put("stream", false)
    }.toString()

    private fun streamRequestBody(model: String, prompt: String): String = buildJsonObject {
        put("model", model)
        putJsonArray("messages") {
            add(
                buildJsonObject {
                    put("role", "user")
                    put("content", prompt)
                },
            )
        }
        put("stream", true)
        put(
            "stream_options",
            buildJsonObject {
                put("include_usage", true)
            },
        )
    }.toString()

    private fun extractCompletionText(payload: String): String {
        val content = try {
            JSON.parseToJsonElement(payload)
                .jsonObject["choices"]
                ?.jsonArray
                ?.firstOrNull()
                ?.jsonObject
                ?.get("message")
                ?.jsonObject
                ?.get("content")
                ?.asTextContent()
        } catch (_: Exception) {
            throw com.bandu.tiji.ai.api.error.AiError.InvalidResponse("openai.invalid_json")
        }
        if (content.isNullOrBlank()) {
            throw com.bandu.tiji.ai.api.error.AiError.InvalidResponse("openai.empty_response")
        }
        return content
    }

    private fun JsonElement.asTextContent(): String? = when (this) {
        is JsonPrimitive -> contentOrNull
        is JsonArray -> mapNotNull { part ->
            runCatching {
                part.jsonObject["text"]?.jsonPrimitive?.contentOrNull
            }.getOrNull()
        }.joinToString(separator = "")
        else -> null
    }

    private fun extractStreamDelta(payload: String): String? = try {
        JSON.parseToJsonElement(payload)
            .jsonObject["choices"]
            ?.jsonArray
            ?.firstOrNull()
            ?.jsonObject
            ?.get("delta")
            ?.jsonObject
            ?.get("content")
            ?.asTextContent()
            ?.takeIf { it.isNotEmpty() }
    } catch (_: Exception) {
        throw com.bandu.tiji.ai.api.error.AiError.InvalidResponse("openai.invalid_sse_json")
    }

    private fun validationFailureCode(error: Throwable): String = when (error) {
        is OpenAiHttpException -> when (error.statusCode) {
            401, 403 -> "authentication"
            408, 504 -> "timeout"
            429 -> "rate_limited"
            in 500..599 -> "network_error"
            else -> "invalid_response"
        }
        is java.net.SocketTimeoutException -> "timeout"
        is IOException -> "network_error"
        else -> "invalid_response"
    }

    private fun ResolvedAiConfiguration.chatCompletionsEndpoint(): HttpUrl =
        baseUrl.toHttpUrl()
            .newBuilder()
            .addPathSegment("chat")
            .addPathSegment("completions")
            .build()

    private companion object {
        const val AUTHORIZATION_HEADER = "Authorization"
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        val JSON = Json {
            ignoreUnknownKeys = true
        }
    }
}

internal class OpenAiHttpException(
    val statusCode: Int,
) : IOException("OpenAI-compatible request failed with HTTP $statusCode")
