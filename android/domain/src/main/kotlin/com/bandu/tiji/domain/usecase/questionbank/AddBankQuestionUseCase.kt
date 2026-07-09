package com.bandu.tiji.domain.usecase.questionbank

import com.bandu.tiji.core.model.id.BankQuestionId
import com.bandu.tiji.core.model.questionbank.BankQuestionDraft
import com.bandu.tiji.domain.repository.QuestionBankRepository

class AddBankQuestionUseCase(
    private val repository: QuestionBankRepository,
) {
    suspend operator fun invoke(draft: BankQuestionDraft): BankQuestionId =
        repository.addQuestion(draft)
}
