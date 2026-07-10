package com.bandu.tiji.core.testing.fake

import com.bandu.tiji.domain.ai.AiStreamEvent
import com.bandu.tiji.domain.ai.AnalyzeImageRequest
import com.bandu.tiji.domain.ai.AnalyzedQuestion
import com.bandu.tiji.domain.ai.ExerciseGrade
import com.bandu.tiji.domain.ai.ExerciseRequest
import com.bandu.tiji.domain.ai.GeneratedExercise
import com.bandu.tiji.domain.ai.GradeExerciseRequest
import com.bandu.tiji.domain.ai.SplitQuestionBankPage
import com.bandu.tiji.domain.ai.SplitQuestionBankPageRequest
import com.bandu.tiji.domain.ai.TutorRequest
import com.bandu.tiji.domain.repository.AiTutorGateway
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

sealed interface CallScript<out T> {
    data class Return<T>(
        val value: T,
    ) : CallScript<T>

    data class Fail(
        val throwable: Throwable,
    ) : CallScript<Nothing>
}

sealed interface TutorStreamScript {
    data class Events(
        val events: List<AiStreamEvent>,
    ) : TutorStreamScript

    data class Fail(
        val eventsBeforeFailure: List<AiStreamEvent> = emptyList(),
        val throwable: Throwable,
    ) : TutorStreamScript

    data class Cancel(
        val eventsBeforeCancellation: List<AiStreamEvent> = emptyList(),
    ) : TutorStreamScript
}

class FakeAiTutorGateway : AiTutorGateway {
    private val analyzeScripts = ArrayDeque<CallScript<AnalyzedQuestion>>()
    private val tutorScripts = ArrayDeque<TutorStreamScript>()
    private val exerciseScripts = ArrayDeque<CallScript<GeneratedExercise>>()
    private val gradeScripts = ArrayDeque<CallScript<ExerciseGrade>>()
    private val splitPageScripts = ArrayDeque<CallScript<SplitQuestionBankPage>>()

    val analyzeRequests = mutableListOf<AnalyzeImageRequest>()
    val tutorRequests = mutableListOf<TutorRequest>()
    val cancelledTutorRequests = mutableListOf<TutorRequest>()
    val exerciseRequests = mutableListOf<ExerciseRequest>()
    val gradeRequests = mutableListOf<GradeExerciseRequest>()
    val splitPageRequests = mutableListOf<SplitQuestionBankPageRequest>()

    fun enqueueAnalyze(script: CallScript<AnalyzedQuestion>) {
        analyzeScripts.addLast(script)
    }

    fun enqueueTutor(script: TutorStreamScript) {
        tutorScripts.addLast(script)
    }

    fun enqueueExercise(script: CallScript<GeneratedExercise>) {
        exerciseScripts.addLast(script)
    }

    fun enqueueGrade(script: CallScript<ExerciseGrade>) {
        gradeScripts.addLast(script)
    }

    fun enqueueSplitPage(script: CallScript<SplitQuestionBankPage>) {
        splitPageScripts.addLast(script)
    }

    override suspend fun analyzeImage(request: AnalyzeImageRequest): AnalyzedQuestion {
        analyzeRequests += request
        return analyzeScripts.removeRequired("analyzeImage").execute()
    }

    override fun streamTutor(request: TutorRequest): Flow<AiStreamEvent> {
        tutorRequests += request
        val script = tutorScripts.removeRequired("streamTutor")
        return flow {
            when (script) {
                is TutorStreamScript.Events -> script.events.forEach { emit(it) }
                is TutorStreamScript.Fail -> {
                    script.eventsBeforeFailure.forEach { emit(it) }
                    throw script.throwable
                }
                is TutorStreamScript.Cancel -> {
                    try {
                        script.eventsBeforeCancellation.forEach { emit(it) }
                        awaitCancellation()
                    } finally {
                        cancelledTutorRequests += request
                    }
                }
            }
        }
    }

    override suspend fun generateExercise(request: ExerciseRequest): GeneratedExercise {
        exerciseRequests += request
        return exerciseScripts.removeRequired("generateExercise").execute()
    }

    override suspend fun gradeExercise(request: GradeExerciseRequest): ExerciseGrade {
        gradeRequests += request
        return gradeScripts.removeRequired("gradeExercise").execute()
    }

    override suspend fun splitQuestionBankPage(
        request: SplitQuestionBankPageRequest,
    ): SplitQuestionBankPage {
        splitPageRequests += request
        return splitPageScripts.removeRequired("splitQuestionBankPage").execute()
    }

    private fun <T> ArrayDeque<T>.removeRequired(operation: String): T =
        removeFirstOrNull() ?: error("No script queued for $operation")

    private fun <T> CallScript<T>.execute(): T =
        when (this) {
            is CallScript.Return -> value
            is CallScript.Fail -> throw throwable
        }
}
