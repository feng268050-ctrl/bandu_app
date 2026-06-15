package com.bandu.tiji.domain.usecase

import app.cash.turbine.test
import com.bandu.tiji.core.model.enums.ExerciseDifficulty
import com.bandu.tiji.core.model.enums.GradeResult
import com.bandu.tiji.core.model.id.ExerciseId
import com.bandu.tiji.core.model.id.TutorSessionId
import com.bandu.tiji.domain.ai.AiStreamEvent
import com.bandu.tiji.domain.ai.ExerciseRequest
import com.bandu.tiji.domain.ai.GradeExerciseRequest
import com.bandu.tiji.domain.fake.FakeAiTutorGateway
import com.bandu.tiji.domain.fake.FakeTutorRepository
import com.bandu.tiji.domain.usecase.tutor.GenerateExerciseUseCase
import com.bandu.tiji.domain.usecase.tutor.GradeExerciseUseCase
import com.bandu.tiji.domain.usecase.tutor.SendTutorMessageUseCase
import com.bandu.tiji.domain.usecase.tutor.StopTutorGenerationUseCase
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

class TutorUseCaseTest {
    private val tutorRepository = FakeTutorRepository()
    private val gateway = FakeAiTutorGateway()

    @Test
    fun sendTutorMessage_streamsAndPersistsAssistantReply() = runTest {
        val sessionId = TutorSessionId("s1")
        val useCase = SendTutorMessageUseCase(tutorRepository, gateway)

        useCase.invoke(sessionId, "请讲解", "question", "history").test {
            assertThat(awaitItem().getOrNull()).isEqualTo(AiStreamEvent.Delta("hello"))
            assertThat(awaitItem().getOrNull()).isEqualTo(AiStreamEvent.Completed)
            awaitComplete()
        }

        assertThat(tutorRepository.userMessages.single().second).isEqualTo("请讲解")
        assertThat(tutorRepository.assistantMessages.single().second).isEqualTo("hello")
        assertThat(gateway.tutorRequests).hasSize(1)
    }

    @Test
    fun stopTutorGeneration_persistsPartialText() = runTest {
        val sessionId = TutorSessionId("s1")
        val result = StopTutorGenerationUseCase(tutorRepository).invoke(sessionId, "partial")

        assertThat(result.isSuccess).isTrue()
        assertThat(tutorRepository.assistantMessages.single().second).isEqualTo("partial")
    }

    @Test
    fun generateExercise_delegatesToGateway() = runTest {
        val request = ExerciseRequest(
            sessionId = TutorSessionId("s1"),
            originalQuestion = "1+1",
            knowledgePoints = "加法",
            difficulty = ExerciseDifficulty.EASY,
        )
        val result = GenerateExerciseUseCase(gateway).invoke(request)

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()?.questionText).isEqualTo("q")
        assertThat(gateway.exerciseRequests).containsExactly(request)
    }

    @Test
    fun gradeExercise_rejectsBlankAnswer() = runTest {
        val request = GradeExerciseRequest(
            exerciseId = ExerciseId("ex-1"),
            exerciseQuestion = "1+1",
            expectedAnswer = "2",
            userAnswer = " ",
        )
        val result = GradeExerciseUseCase(gateway).invoke(request)

        assertThat(result.isSuccess).isFalse()
        assertThat(gateway.gradeRequests).isEmpty()
    }

    @Test
    fun gradeExercise_returnsGatewayResult() = runTest {
        val request = GradeExerciseRequest(
            exerciseId = ExerciseId("ex-1"),
            exerciseQuestion = "1+1",
            expectedAnswer = "2",
            userAnswer = "2",
        )
        val result = GradeExerciseUseCase(gateway).invoke(request)

        assertThat(result.getOrNull()?.result).isEqualTo(GradeResult.CORRECT)
    }
}
