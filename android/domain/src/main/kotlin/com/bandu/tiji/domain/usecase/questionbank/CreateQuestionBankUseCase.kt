package com.bandu.tiji.domain.usecase.questionbank

import com.bandu.tiji.core.model.id.QuestionBankId
import com.bandu.tiji.core.model.questionbank.QuestionBankDraft
import com.bandu.tiji.domain.repository.QuestionBankRepository

class CreateQuestionBankUseCase(
    private val repository: QuestionBankRepository,
) {
    suspend operator fun invoke(draft: QuestionBankDraft): QuestionBankId =
        repository.createBank(draft)
}
