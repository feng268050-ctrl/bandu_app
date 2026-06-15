package com.bandu.tiji.core.testing.fake

import app.cash.turbine.test
import com.bandu.tiji.core.model.enums.ExerciseDifficulty
import com.bandu.tiji.core.model.enums.GradeResult
import com.bandu.tiji.core.model.enums.MistakeStatus
import com.bandu.tiji.core.model.id.ExerciseId
import com.bandu.tiji.core.model.id.TutorSessionId
import com.bandu.tiji.domain.ai.AiStreamEvent
import com.bandu.tiji.domain.ai.AnalyzeImageRequest
import com.bandu.tiji.domain.ai.AnalyzedQuestion
import com.bandu.tiji.domain.ai.ExerciseGrade
import com.bandu.tiji.domain.ai.ExerciseRequest
import com.bandu.tiji.domain.ai.GeneratedExercise
import com.bandu.tiji.domain.ai.GradeExerciseRequest
import com.bandu.tiji.domain.ai.TutorRequest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Test

class FakeAiTutorGatewayTest {
    @Test
    fun `non-streaming scripts return values and record requests`() = runTest {
        val gateway = FakeAiTutorGateway()
        val analysis =
            AnalyzedQuestion(
                subject = "Math",
                knowledgePoints = listOf("Algebra"),
                requiresImage = false,
                wrongAnswerText = "",
                mistakeStatus = MistakeStatus.UNKNOWN,
                mistakeAnalysis = "",
                questionText = "1 + 1",
                answerText = "2",
                analysis = "Addition",
            )
        val exercise = GeneratedExercise("2 + 2", "4", "Addition")
        val grade = ExerciseGrade(GradeResult.CORRECT, "Correct", 0.99)
        gateway.enqueueAnalyze(CallScript.Return(analysis))
        gateway.enqueueExercise(CallScript.Return(exercise))
        gateway.enqueueGrade(CallScript.Return(grade))

        val analyzeRequest = AnalyzeImageRequest(byteArrayOf(1, 2, 3))
        val exerciseRequest =
            ExerciseRequest(
                sessionId = TutorSessionId("session-1"),
                originalQuestion = "1 + 1",
                knowledgePoints = "Addition",
                difficulty = ExerciseDifficulty.EASY,
            )
        val gradeRequest =
            GradeExerciseRequest(
                exerciseId = ExerciseId("exercise-1"),
                exerciseQuestion = "2 + 2",
                expectedAnswer = "4",
                userAnswer = "4",
            )

        assertThat(gateway.analyzeImage(analyzeRequest)).isEqualTo(analysis)
        assertThat(gateway.generateExercise(exerciseRequest)).isEqualTo(exercise)
        assertThat(gateway.gradeExercise(gradeRequest)).isEqualTo(grade)
        assertThat(gateway.analyzeRequests).containsExactly(analyzeRequest)
        assertThat(gateway.exerciseRequests).containsExactly(exerciseRequest)
        assertThat(gateway.gradeRequests).containsExactly(gradeRequest)
    }

    @Test
    fun `failure scripts throw configured errors`() = runTest {
        val gateway = FakeAiTutorGateway()
        gateway.enqueueAnalyze(CallScript.Fail(IllegalStateException("analysis failed")))
        gateway.enqueueExercise(CallScript.Fail(IllegalArgumentException("exercise failed")))

        val analyzeFailure =
            runCatching { gateway.analyzeImage(AnalyzeImageRequest(byteArrayOf(1))) }.exceptionOrNull()
        val exerciseFailure =
            runCatching {
                gateway.generateExercise(
                    ExerciseRequest(
                        sessionId = TutorSessionId("session-1"),
                        originalQuestion = "question",
                        knowledgePoints = "point",
                        difficulty = ExerciseDifficulty.MEDIUM,
                    ),
                )
            }.exceptionOrNull()

        assertThat(analyzeFailure).hasMessageThat().isEqualTo("analysis failed")
        assertThat(exerciseFailure).hasMessageThat().isEqualTo("exercise failed")
    }

    @Test
    fun `incremental tutor script emits events in order`() = runTest {
        val gateway = FakeAiTutorGateway()
        val request = tutorRequest()
        gateway.enqueueTutor(
            TutorStreamScript.Events(
                listOf(
                    AiStreamEvent.Delta("First"),
                    AiStreamEvent.Delta(" second"),
                    AiStreamEvent.Completed,
                ),
            ),
        )

        gateway.streamTutor(request).test {
            assertThat(awaitItem()).isEqualTo(AiStreamEvent.Delta("First"))
            assertThat(awaitItem()).isEqualTo(AiStreamEvent.Delta(" second"))
            assertThat(awaitItem()).isEqualTo(AiStreamEvent.Completed)
            awaitComplete()
        }
        assertThat(gateway.tutorRequests).containsExactly(request)
    }

    @Test
    fun `tutor failure script emits partial events before error`() = runTest {
        val gateway = FakeAiTutorGateway()
        gateway.enqueueTutor(
            TutorStreamScript.Fail(
                eventsBeforeFailure = listOf(AiStreamEvent.Delta("Partial")),
                throwable = IllegalStateException("stream failed"),
            ),
        )

        gateway.streamTutor(tutorRequest()).test {
            assertThat(awaitItem()).isEqualTo(AiStreamEvent.Delta("Partial"))
            assertThat(awaitError()).hasMessageThat().isEqualTo("stream failed")
        }
    }

    @Test
    fun `cancellation script waits until collector cancels and records cancellation`() = runTest {
        val gateway = FakeAiTutorGateway()
        val request = tutorRequest()
        val emitted = CompletableDeferred<Unit>()
        gateway.enqueueTutor(
            TutorStreamScript.Cancel(
                eventsBeforeCancellation = listOf(AiStreamEvent.Delta("Partial")),
            ),
        )

        val job =
            launch {
                gateway
                    .streamTutor(request)
                    .onEach { emitted.complete(Unit) }
                    .collect {}
            }
        emitted.await()
        job.cancelAndJoin()

        assertThat(gateway.cancelledTutorRequests).containsExactly(request)
    }

    @Test
    fun `missing script fails with operation name`() = runTest {
        val gateway = FakeAiTutorGateway()

        val failure = runCatching { gateway.gradeExercise(gradeRequest()) }.exceptionOrNull()

        assertThat(failure).hasMessageThat().contains("gradeExercise")
    }

    private fun tutorRequest(): TutorRequest =
        TutorRequest(
            sessionId = TutorSessionId("session-1"),
            userMessage = "Help",
            questionContext = "Question",
            conversationContext = "",
        )

    private fun gradeRequest(): GradeExerciseRequest =
        GradeExerciseRequest(
            exerciseId = ExerciseId("exercise-1"),
            exerciseQuestion = "2 + 2",
            expectedAnswer = "4",
            userAnswer = "4",
        )
}
