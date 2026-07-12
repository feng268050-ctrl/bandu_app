package com.bandu.tiji.domain.repository

import com.bandu.tiji.core.model.id.BankQuestionId
import com.bandu.tiji.core.model.id.ExamAttemptId
import com.bandu.tiji.core.model.id.ExamSessionId
import com.bandu.tiji.core.model.id.QuestionBankId
import com.bandu.tiji.core.model.questionbank.BankQuestionDraft
import com.bandu.tiji.core.model.questionbank.ExamSession
import com.bandu.tiji.core.model.questionbank.ExamSessionSummary
import com.bandu.tiji.core.model.questionbank.QuestionBank
import com.bandu.tiji.core.model.questionbank.QuestionBankDraft
import com.bandu.tiji.core.model.questionbank.QuestionBankSummary
import kotlinx.coroutines.flow.Flow

interface QuestionBankRepository {
    fun observeBanks(): Flow<List<QuestionBankSummary>>

    fun observeBank(id: QuestionBankId): Flow<QuestionBank?>

    fun observeExamSession(id: ExamSessionId): Flow<ExamSession?>

    fun observeExamSessions(bankId: QuestionBankId): Flow<List<ExamSessionSummary>>

    suspend fun createBank(draft: QuestionBankDraft): QuestionBankId

    suspend fun deleteBank(id: QuestionBankId)

    suspend fun addQuestion(draft: BankQuestionDraft): BankQuestionId

    suspend fun addQuestions(drafts: List<BankQuestionDraft>): List<BankQuestionId>

    suspend fun createExam(bankId: QuestionBankId, questionCount: Int): ExamSessionId

    suspend fun renameExam(sessionId: ExamSessionId, title: String)

    suspend fun deleteExam(sessionId: ExamSessionId)

    suspend fun submitAnswer(attemptId: ExamAttemptId, userAnswer: String)

    suspend fun completeExam(sessionId: ExamSessionId)
}
