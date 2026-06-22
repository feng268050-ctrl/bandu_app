package com.bandu.tiji.domain.usecase.tutor

import com.bandu.tiji.core.common.result.AppError
import com.bandu.tiji.core.common.result.AppResult
import com.bandu.tiji.core.model.id.TutorSessionId
import com.bandu.tiji.domain.ai.AiStreamEvent
import com.bandu.tiji.domain.ai.TutorRequest
import com.bandu.tiji.domain.repository.AiTutorGateway
import com.bandu.tiji.domain.repository.TutorRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.CancellationException

class SendTutorMessageUseCase(
    private val tutorRepository: TutorRepository,
    private val aiTutorGateway: AiTutorGateway,
) {
    fun invoke(
        sessionId: TutorSessionId,
        text: String,
        questionContext: String,
        conversationContext: String,
        gradeInstruction: String = "",
    ): Flow<AppResult<AiStreamEvent>> = flow {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            emit(AppResult.Failure(AppError.Validation("tutor.message.blank")))
            return@flow
        }

        try {
            tutorRepository.appendUserMessage(sessionId, trimmed)
            val request = TutorRequest(
                sessionId = sessionId,
                userMessage = trimmed,
                questionContext = questionContext,
                conversationContext = conversationContext,
                gradeInstruction = gradeInstruction,
            )
            val buffer = StringBuilder()
            aiTutorGateway.streamTutor(request).collect { event ->
                when (event) {
                    is AiStreamEvent.Delta -> buffer.append(event.text)
                    is AiStreamEvent.Completed -> {
                        tutorRepository.appendAssistantMessage(sessionId, buffer.toString())
                    }
                    is AiStreamEvent.Usage -> Unit
                }
                emit(AppResult.Success(event))
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            emit(AppResult.Failure(AppError.Unexpected(error)))
        }
    }
}
