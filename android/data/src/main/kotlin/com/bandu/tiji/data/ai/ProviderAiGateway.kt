package com.bandu.tiji.data.ai

import com.bandu.tiji.ai.api.config.ProviderValidation
import com.bandu.tiji.ai.api.config.ResolvedAiConfiguration
import com.bandu.tiji.ai.api.error.AiError
import com.bandu.tiji.ai.api.provider.AiProvider
import com.bandu.tiji.ai.api.provider.AiProviderRegistry
import com.bandu.tiji.core.storage.preferences.DevicePreferencesStore
import com.bandu.tiji.core.storage.preferences.PortablePreferencesStore
import com.bandu.tiji.domain.ai.AiConfigurationDraft
import com.bandu.tiji.domain.ai.AiGatewayException
import com.bandu.tiji.domain.ai.AiStreamEvent
import com.bandu.tiji.domain.ai.AnalyzeImageRequest
import com.bandu.tiji.domain.ai.AnalyzedQuestion
import com.bandu.tiji.domain.ai.ExerciseGrade
import com.bandu.tiji.domain.ai.ExerciseRequest
import com.bandu.tiji.domain.ai.GeneratedExercise
import com.bandu.tiji.domain.ai.GradeExerciseRequest
import com.bandu.tiji.domain.ai.TutorRequest
import com.bandu.tiji.domain.ai.ValidationResult
import com.bandu.tiji.domain.repository.AiTutorGateway
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first

@Singleton
class ProviderAiTutorGateway @Inject constructor(
    private val registry: AiProviderRegistry,
    private val configurationResolver: ActiveAiConfigurationResolver,
) : AiTutorGateway {
    override suspend fun analyzeImage(request: AnalyzeImageRequest): AnalyzedQuestion =
        normalizedCall {
            val configuration = configurationResolver.resolve()
            registry.require(configuration.providerType)
                .analyzeImage(configuration, request.toApi())
                .toDomain()
        }

    override fun streamTutor(request: TutorRequest): Flow<AiStreamEvent> = flow {
        try {
            val configuration = configurationResolver.resolve()
            registry.require(configuration.providerType)
                .streamTutor(configuration, request.toApi())
                .collect { emit(it.toDomain()) }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            throw error.toDomainAiError()
        }
    }

    override suspend fun generateExercise(request: ExerciseRequest): GeneratedExercise =
        normalizedCall {
            val configuration = configurationResolver.resolve()
            registry.require(configuration.providerType)
                .generateExercise(configuration, request.toApi())
                .toDomain()
        }

    override suspend fun gradeExercise(request: GradeExerciseRequest): ExerciseGrade =
        normalizedCall {
            val configuration = configurationResolver.resolve()
            registry.require(configuration.providerType)
                .gradeExercise(configuration, request.toApi())
                .toDomain()
        }
}

@Singleton
class StorageActiveAiConfigurationResolver @Inject constructor(
    private val portablePreferences: PortablePreferencesStore,
    private val devicePreferences: DevicePreferencesStore,
    private val apiKeyStore: ApiKeyCipherStore,
) : ActiveAiConfigurationResolver {
    override suspend fun resolve(): ResolvedAiConfiguration {
        val portable = portablePreferences.data.first()
        val device = devicePreferences.data.first()
        val providerType = portable.providerType
            ?: throw AiGatewayException.ConfigurationRequired
        val keyChars = apiKeyStore.read()
            ?: throw AiGatewayException.ConfigurationRequired
        val apiKey = try {
            keyChars.concatToString()
        } finally {
            keyChars.fill('\u0000')
        }
        return runCatching {
            ResolvedAiConfiguration(
                id = "configuration-${providerType.name.lowercase()}",
                displayName = portable.providerDisplayName,
                providerType = providerType,
                baseUrl = portable.baseUrl,
                apiKey = apiKey,
                analysisModel = portable.analysisModel,
                tutorModel = portable.tutorModel,
                allowPrivateCleartext = device.allowPrivateHttp,
            )
        }.getOrElse {
            throw AiGatewayException.ConfigurationRequired
        }
    }
}

fun interface ActiveAiConfigurationResolver {
    suspend fun resolve(): ResolvedAiConfiguration
}

@Singleton
class RegistryAiConfigurationValidationGateway @Inject constructor(
    private val registry: AiProviderRegistry,
) : AiConfigurationValidationGateway {
    override suspend fun validate(
        draft: AiConfigurationDraft,
        apiKey: CharArray,
    ): ValidationResult {
        val configuration = ResolvedAiConfiguration(
            id = "configuration-${draft.providerType.name.lowercase()}",
            displayName = draft.displayName,
            providerType = draft.providerType,
            baseUrl = draft.baseUrl,
            apiKey = apiKey.concatToString(),
            analysisModel = draft.analysisModel,
            tutorModel = draft.tutorModel,
            allowPrivateCleartext = draft.allowPrivateCleartext,
        )
        return when (val result = registry.require(draft.providerType).validate(configuration)) {
            ProviderValidation.Success -> ValidationResult.Success
            is ProviderValidation.Failure -> ValidationResult.Failure(listOf(result.message))
        }
    }
}

private suspend inline fun <T> normalizedCall(crossinline block: suspend () -> T): T =
    try {
        block()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Throwable) {
        throw error.toDomainAiError()
    }

private fun Throwable.toDomainAiError(): Throwable =
    when (this) {
        is AiGatewayException -> this
        AiError.ConfigurationRequired -> AiGatewayException.ConfigurationRequired
        AiError.Authentication -> AiGatewayException.Authentication
        AiError.RateLimited -> AiGatewayException.RateLimited
        AiError.Timeout -> AiGatewayException.Timeout
        AiError.NetworkUnavailable -> AiGatewayException.NetworkUnavailable
        is AiError.InvalidResponse -> AiGatewayException.InvalidResponse(diagnosticCode)
        is AiError.EndpointRejected -> AiGatewayException.EndpointRejected(reason)
        AiError.Cancelled -> CancellationException("AI operation cancelled")
        else -> AiGatewayException.InvalidResponse("unexpected:${this::class.simpleName}")
    }

private fun AnalyzeImageRequest.toApi() =
    com.bandu.tiji.ai.api.model.AnalyzeImageRequest(
        imageBytes = imageBytes,
        mimeType = mimeType,
        gradeInstruction = gradeInstruction,
        knowledgePointsList = knowledgePointsList,
        languageInstruction = languageInstruction,
        providerHints = providerHints,
    )

private fun com.bandu.tiji.ai.api.model.AnalyzedQuestion.toDomain() =
    AnalyzedQuestion(
        subject = subject,
        knowledgePoints = knowledgePoints,
        requiresImage = requiresImage,
        wrongAnswerText = wrongAnswerText,
        mistakeStatus = mistakeStatus,
        mistakeAnalysis = mistakeAnalysis,
        questionText = questionText,
        answerText = answerText,
        analysis = analysis,
    )

private fun TutorRequest.toApi() =
    com.bandu.tiji.ai.api.model.TutorRequest(
        sessionId = sessionId,
        userMessage = userMessage,
        questionContext = questionContext,
        conversationContext = conversationContext,
        gradeInstruction = gradeInstruction,
    )

private fun com.bandu.tiji.ai.api.model.AiStreamEvent.toDomain(): AiStreamEvent =
    when (this) {
        is com.bandu.tiji.ai.api.model.AiStreamEvent.Delta -> AiStreamEvent.Delta(text)
        is com.bandu.tiji.ai.api.model.AiStreamEvent.Usage ->
            AiStreamEvent.Usage(inputTokens, outputTokens)
        com.bandu.tiji.ai.api.model.AiStreamEvent.Completed -> AiStreamEvent.Completed
    }

private fun ExerciseRequest.toApi() =
    com.bandu.tiji.ai.api.model.ExerciseRequest(
        sessionId = sessionId,
        originalQuestion = originalQuestion,
        knowledgePoints = knowledgePoints,
        difficulty = difficulty,
        gradeInstruction = gradeInstruction,
        sourceErrorItemId = sourceErrorItemId,
        subject = subject,
    )

private fun com.bandu.tiji.ai.api.model.GeneratedExercise.toDomain() =
    GeneratedExercise(questionText, answerText, analysis)

private fun GradeExerciseRequest.toApi() =
    com.bandu.tiji.ai.api.model.GradeExerciseRequest(
        exerciseId = exerciseId,
        exerciseQuestion = exerciseQuestion,
        expectedAnswer = expectedAnswer,
        userAnswer = userAnswer,
        rubricContext = rubricContext,
    )

private fun com.bandu.tiji.ai.api.model.ExerciseGrade.toDomain() =
    ExerciseGrade(
        result = result,
        feedback = feedback,
        confidence = confidence,
    )
