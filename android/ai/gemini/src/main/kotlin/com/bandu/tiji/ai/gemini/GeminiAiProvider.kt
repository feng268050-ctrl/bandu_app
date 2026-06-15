package com.bandu.tiji.ai.gemini

import com.bandu.tiji.ai.api.config.ProviderValidation
import com.bandu.tiji.ai.api.config.ResolvedAiConfiguration
import com.bandu.tiji.ai.api.error.AiError
import com.bandu.tiji.ai.api.model.AiStreamEvent
import com.bandu.tiji.ai.api.model.AnalyzeImageRequest
import com.bandu.tiji.ai.api.model.AnalyzedQuestion
import com.bandu.tiji.ai.api.model.ExerciseGrade
import com.bandu.tiji.ai.api.model.ExerciseRequest
import com.bandu.tiji.ai.api.model.GeneratedExercise
import com.bandu.tiji.ai.api.model.GradeExerciseRequest
import com.bandu.tiji.ai.api.model.TutorRequest
import com.bandu.tiji.ai.api.parser.ExerciseResponseParser
import com.bandu.tiji.ai.api.parser.ImageAnalysisResponseParser
import com.bandu.tiji.ai.api.prompt.PromptRenderer
import com.bandu.tiji.ai.api.prompt.PromptType
import com.bandu.tiji.ai.api.provider.AiProvider
import com.bandu.tiji.ai.api.retry.AiOperationType
import com.bandu.tiji.ai.api.retry.RetryContext
import com.bandu.tiji.ai.api.retry.RetryDecision
import com.bandu.tiji.ai.api.retry.RetryPolicy
import com.bandu.tiji.core.model.enums.AiProviderType
import com.bandu.tiji.core.network.AiHttpOperation
import com.bandu.tiji.core.network.HttpEngine
import com.bandu.tiji.core.network.retry.ProviderRetryDecision
import com.bandu.tiji.core.network.retry.ProviderRetryExecutor
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
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

fun interface GeminiHttpEngineFactory {
    fun create(
        configuration: ResolvedAiConfiguration,
        operation: AiHttpOperation,
    ): HttpEngine
}

class GeminiAiProvider(
    private val httpEngineFactory: GeminiHttpEngineFactory = GeminiHttpEngineFactory { _, _ ->
        error("GeminiHttpEngineFactory is not configured")
    },
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val promptTemplates: GeminiPromptTemplateSource = DefaultGeminiPromptTemplates,
    private val promptRenderer: PromptRenderer = PromptRenderer(),
    private val imageAnalysisParser: ImageAnalysisResponseParser = ImageAnalysisResponseParser(),
    private val exerciseParser: ExerciseResponseParser = ExerciseResponseParser(),
    private val retryPolicy: RetryPolicy = RetryPolicy(),
    private val retryExecutor: ProviderRetryExecutor = ProviderRetryExecutor(),
) : AiProvider {
    override val type: AiProviderType = AiProviderType.GEMINI

    override suspend fun validate(configuration: ResolvedAiConfiguration): ProviderValidation =
        withContext(ioDispatcher) {
            try {
                val request = Request.Builder()
                    .url(configuration.endpoint(configuration.analysisModel, GENERATE_CONTENT))
                    .header(API_KEY_HEADER, configuration.apiKey)
                    .post(validationBody().toRequestBody(JSON_MEDIA_TYPE))
                    .build()
                httpEngineFactory
                    .create(configuration, AiHttpOperation.CONFIGURATION_VALIDATION)
                    .newCall(request)
                    .execute()
                    .use { response ->
                        if (!response.isSuccessful) throw GeminiHttpException(response.code)
                        ProviderValidation.Success
                    }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                ProviderValidation.Failure(GeminiErrorMapper.validationCode(error))
            }
        }

    override suspend fun analyzeImage(
        configuration: ResolvedAiConfiguration,
        request: AnalyzeImageRequest,
    ): AnalyzedQuestion {
        val prompt = renderPrompt(
            PromptType.ANALYZE_IMAGE,
            mapOf(
                "language_instruction" to request.languageInstruction,
                "knowledge_points_list" to request.knowledgePointsList,
                "grade_instruction" to request.gradeInstruction,
                "provider_hints" to request.providerHints,
            ),
        )
        return executeWithRetry(AiOperationType.ANALYZE_IMAGE) {
            imageAnalysisParser.parse(
                generateImageAnalysisText(configuration, request, prompt),
            )
        }
    }

    override fun streamTutor(
        configuration: ResolvedAiConfiguration,
        request: TutorRequest,
    ): Flow<AiStreamEvent> = retryingTutorStream(
        source = {
            streamText(
                configuration = configuration,
                model = configuration.tutorModel,
                prompt = renderPrompt(
                    PromptType.TUTOR,
                    mapOf(
                        "question_context" to request.questionContext,
                        "conversation_context" to request.conversationContext,
                        "user_message" to request.userMessage,
                        "grade_instruction" to request.gradeInstruction,
                    ),
                ),
            )
        },
    )

    override suspend fun generateExercise(
        configuration: ResolvedAiConfiguration,
        request: ExerciseRequest,
    ): GeneratedExercise {
        return executeWithRetry(AiOperationType.GENERATE_EXERCISE) {
            val response = generateText(
                configuration = configuration,
                model = configuration.tutorModel,
                prompt = renderPrompt(
                    PromptType.GENERATE_EXERCISE,
                    mapOf(
                        "original_question" to request.originalQuestion,
                        "knowledge_points" to request.knowledgePoints,
                        "difficulty_level" to request.difficulty.name.lowercase(),
                        "grade_instruction" to request.gradeInstruction,
                    ),
                ),
                operation = AiHttpOperation.EXERCISE,
            )
            exerciseParser.parseGeneratedExercise(response)
        }
    }

    override suspend fun gradeExercise(
        configuration: ResolvedAiConfiguration,
        request: GradeExerciseRequest,
    ): ExerciseGrade {
        return executeWithRetry(AiOperationType.GRADE_EXERCISE) {
            val response = generateText(
                configuration = configuration,
                model = configuration.tutorModel,
                prompt = renderPrompt(
                    PromptType.GRADE_EXERCISE,
                    mapOf(
                        "exercise_question" to request.exerciseQuestion,
                        "expected_answer" to request.expectedAnswer,
                        "user_answer" to request.userAnswer,
                        "rubric_context" to request.rubricContext,
                    ),
                ),
                operation = AiHttpOperation.EXERCISE,
            )
            exerciseParser.parseExerciseGrade(response)
        }
    }

    internal suspend fun generateText(
        configuration: ResolvedAiConfiguration,
        model: String,
        prompt: String,
        operation: AiHttpOperation,
    ): String = executeGenerateContent(
        configuration = configuration,
        model = model,
        operation = operation,
        body = textRequestBody(prompt),
    )

    internal suspend fun generateImageAnalysisText(
        configuration: ResolvedAiConfiguration,
        request: AnalyzeImageRequest,
        prompt: String,
    ): String {
        require(request.mimeType == JPEG_MIME_TYPE) {
            "Gemini image analysis requires processed JPEG input"
        }
        return executeGenerateContent(
            configuration = configuration,
            model = configuration.analysisModel,
            operation = AiHttpOperation.IMAGE_ANALYSIS,
            body = imageRequestBody(request, prompt),
        )
    }

    internal fun streamText(
        configuration: ResolvedAiConfiguration,
        model: String,
        prompt: String,
    ): Flow<AiStreamEvent> = callbackFlow {
        val request = Request.Builder()
            .url(
                configuration.endpoint(model, STREAM_GENERATE_CONTENT)
                    .newBuilder()
                    .addQueryParameter("alt", "sse")
                    .build(),
            )
            .header(API_KEY_HEADER, configuration.apiKey)
            .post(textRequestBody(prompt).toRequestBody(JSON_MEDIA_TYPE))
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
                                    throw GeminiHttpException(response.code)
                                }
                                SseReader().readEach(response.body.source()) { event ->
                                    when (event) {
                                        is SseEvent.Data -> extractDelta(event.data)?.let { delta ->
                                            send(AiStreamEvent.Delta(delta))
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

    private suspend fun executeGenerateContent(
        configuration: ResolvedAiConfiguration,
        model: String,
        operation: AiHttpOperation,
        body: String,
    ): String = withContext(ioDispatcher) {
        val request = Request.Builder()
            .url(configuration.endpoint(model, GENERATE_CONTENT))
            .header(API_KEY_HEADER, configuration.apiKey)
            .post(body.toRequestBody(JSON_MEDIA_TYPE))
            .build()
        httpEngineFactory.create(configuration, operation)
            .newCall(request)
            .execute()
            .use { response ->
                if (!response.isSuccessful) {
                    throw GeminiHttpException(response.code)
                }
                extractText(response.body.string())
            }
    }

    private fun renderPrompt(
        type: PromptType,
        values: Map<String, String>,
    ): String = promptRenderer.render(promptTemplates.template(type), values)

    private suspend fun <T> executeWithRetry(
        operation: AiOperationType,
        block: suspend () -> T,
    ): T = try {
        retryExecutor.execute(
            decide = { failure, failedAttempt ->
                retryPolicy.decide(
                    RetryContext(
                        operation = operation,
                        attempt = failedAttempt,
                        error = GeminiErrorMapper.map(failure),
                        httpStatus = (failure as? GeminiHttpException)?.statusCode,
                    ),
                ).toProviderDecision()
            },
            block = { _ -> block() },
        )
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Throwable) {
        throw GeminiErrorMapper.map(error)
    }

    private fun retryingTutorStream(
        source: () -> Flow<AiStreamEvent>,
    ): Flow<AiStreamEvent> = flow {
        var attempt = 0
        while (true) {
            var receivedDelta = false
            try {
                source().collect { event ->
                    if (event is AiStreamEvent.Delta) receivedDelta = true
                    emit(event)
                }
                return@flow
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Throwable) {
                val decision = retryPolicy.decide(
                    RetryContext(
                        operation = AiOperationType.STREAM_TUTOR_FIRST_BYTE,
                        attempt = attempt,
                        error = GeminiErrorMapper.map(failure),
                        httpStatus = (failure as? GeminiHttpException)?.statusCode,
                        receivedStreamDelta = receivedDelta,
                    ),
                )
                when (decision) {
                    RetryDecision.DoNotRetry -> throw GeminiErrorMapper.map(failure)
                    is RetryDecision.RetryAfter -> {
                        if (decision.delayMillis > 0) {
                            kotlinx.coroutines.delay(decision.delayMillis)
                        }
                        attempt += 1
                    }
                }
            }
        }
    }

    private fun RetryDecision.toProviderDecision(): ProviderRetryDecision = when (this) {
        RetryDecision.DoNotRetry -> ProviderRetryDecision.DoNotRetry
        is RetryDecision.RetryAfter -> ProviderRetryDecision.RetryAfter(delayMillis)
    }

    private fun validationBody(): String = buildJsonObject {
        putJsonArray("contents") {
            add(
                buildJsonObject {
                    putJsonArray("parts") {
                        add(buildJsonObject { put("text", "ping") })
                    }
                },
            )
        }
        putJsonObject("generationConfig") {
            put("maxOutputTokens", 1)
        }
    }.toString()

    private fun textRequestBody(prompt: String): String = buildJsonObject {
        putJsonArray("contents") {
            add(
                buildJsonObject {
                    put("role", "user")
                    putJsonArray("parts") {
                        add(buildJsonObject { put("text", prompt) })
                    }
                },
            )
        }
    }.toString()

    private fun imageRequestBody(
        request: AnalyzeImageRequest,
        prompt: String,
    ): String = buildJsonObject {
        putJsonArray("contents") {
            add(
                buildJsonObject {
                    put("role", "user")
                    putJsonArray("parts") {
                        add(buildJsonObject { put("text", prompt) })
                        add(
                            buildJsonObject {
                                putJsonObject("inline_data") {
                                    put("mime_type", JPEG_MIME_TYPE)
                                    put(
                                        "data",
                                        Base64.getEncoder().encodeToString(request.imageBytes),
                                    )
                                }
                            },
                        )
                    }
                },
            )
        }
    }.toString()

    private fun extractText(payload: String): String {
        val text = extractCandidateText(payload)
        if (text.isNullOrBlank()) {
            throw AiError.InvalidResponse("gemini.empty_response")
        }
        return text
    }

    private fun extractDelta(payload: String): String? =
        extractCandidateText(payload)?.takeIf { it.isNotEmpty() }

    private fun extractCandidateText(payload: String): String? = runCatching {
            JSON.parseToJsonElement(payload)
                .jsonObject["candidates"]
                ?.jsonArray
                ?.flatMap { candidate ->
                    candidate.jsonObject["content"]
                        ?.jsonObject
                        ?.get("parts")
                        ?.jsonArray
                        ?.mapNotNull { part ->
                            part.jsonObject["text"]?.jsonPrimitive?.contentOrNull
                        }
                        .orEmpty()
                }
                ?.joinToString(separator = "")
        }.getOrElse {
            throw AiError.InvalidResponse("gemini.invalid_json")
        }

    private fun ResolvedAiConfiguration.endpoint(model: String, operation: String): HttpUrl =
        baseUrl.toHttpUrl()
            .newBuilder()
            .addPathSegment("v1beta")
            .addPathSegment("models")
            .addPathSegment("$model:$operation")
            .build()

    private companion object {
        const val API_KEY_HEADER = "x-goog-api-key"
        const val GENERATE_CONTENT = "generateContent"
        const val STREAM_GENERATE_CONTENT = "streamGenerateContent"
        const val JPEG_MIME_TYPE = "image/jpeg"
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        val JSON = kotlinx.serialization.json.Json {
            ignoreUnknownKeys = true
        }
    }
}

internal class GeminiHttpException(
    val statusCode: Int,
) : IOException("Gemini request failed with HTTP $statusCode")
