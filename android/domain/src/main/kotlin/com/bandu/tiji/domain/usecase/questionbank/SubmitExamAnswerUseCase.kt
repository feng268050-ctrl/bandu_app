package com.bandu.tiji.domain.usecase.questionbank

import com.bandu.tiji.core.model.id.ExamAttemptId
import com.bandu.tiji.domain.repository.QuestionBankRepository

class SubmitExamAnswerUseCase(
    private val repository: QuestionBankRepository,
) {
    suspend operator fun invoke(attemptId: ExamAttemptId, userAnswer: String) {
        repository.submitAnswer(attemptId, userAnswer)
    }
}
