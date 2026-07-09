package com.bandu.tiji.domain.usecase.questionbank

import com.bandu.tiji.core.model.id.ExamSessionId
import com.bandu.tiji.core.model.id.QuestionBankId
import com.bandu.tiji.domain.repository.QuestionBankRepository

class CreateExamSessionUseCase(
    private val repository: QuestionBankRepository,
) {
    suspend operator fun invoke(bankId: QuestionBankId, questionCount: Int): ExamSessionId =
        repository.createExam(bankId, questionCount)
}
