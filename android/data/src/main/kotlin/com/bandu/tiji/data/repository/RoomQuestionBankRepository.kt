package com.bandu.tiji.data.repository

import androidx.room.withTransaction
import com.bandu.tiji.core.common.id.UuidGenerator
import com.bandu.tiji.core.common.time.Clock
import com.bandu.tiji.core.model.id.BankQuestionId
import com.bandu.tiji.core.model.id.ExamAttemptId
import com.bandu.tiji.core.model.id.ExamSessionId
import com.bandu.tiji.core.model.id.QuestionBankId
import com.bandu.tiji.core.model.questionbank.BankQuestionDraft
import com.bandu.tiji.core.model.questionbank.BankQuestionType
import com.bandu.tiji.core.model.questionbank.ExamGradingResult
import com.bandu.tiji.core.model.questionbank.ExamGradingSource
import com.bandu.tiji.core.model.questionbank.ExamSession
import com.bandu.tiji.core.model.questionbank.ExamSessionStatus
import com.bandu.tiji.core.model.questionbank.ExamSessionSummary
import com.bandu.tiji.core.model.questionbank.QuestionBank
import com.bandu.tiji.core.model.questionbank.QuestionBankDraft
import com.bandu.tiji.core.model.questionbank.QuestionBankImportStatus
import com.bandu.tiji.core.model.questionbank.QuestionBankSummary
import com.bandu.tiji.core.storage.db.LearningDatabase
import com.bandu.tiji.core.storage.db.entity.BankQuestionEntity
import com.bandu.tiji.data.mapper.examAttemptEntity
import com.bandu.tiji.data.mapper.examSessionEntity
import com.bandu.tiji.data.mapper.toDomain
import com.bandu.tiji.data.mapper.toEntity
import com.bandu.tiji.domain.repository.QuestionBankRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

@Singleton
class RoomQuestionBankRepository @Inject constructor(
    private val database: LearningDatabase,
    private val uuidGenerator: UuidGenerator,
    private val clock: Clock,
) : QuestionBankRepository {
    private val dao = database.questionBankDao()

    override fun observeBanks(): Flow<List<QuestionBankSummary>> =
        dao.observeBankSummaries().map { banks ->
            banks.map { it.toDomain() }
        }

    override fun observeBank(id: QuestionBankId): Flow<QuestionBank?> =
        combine(
            dao.observeBank(id.value),
            dao.observeQuestions(id.value),
        ) { bank, questions ->
            bank?.toDomain(questions.map { it.toDomain() })
        }

    override fun observeExamSession(id: ExamSessionId): Flow<ExamSession?> =
        combine(
            dao.observeSession(id.value),
            dao.observeAttemptQuestions(id.value),
        ) { session, attempts ->
            session?.toDomain(attempts.map { it.toDomain() })
        }

    override fun observeExamSessions(bankId: QuestionBankId): Flow<List<ExamSessionSummary>> =
        dao.observeSessionSummaries(bankId.value).map { sessions ->
            sessions.map { it.toDomain() }
        }

    override suspend fun createBank(draft: QuestionBankDraft): QuestionBankId {
        val id = QuestionBankId(uuidGenerator.newUuid())
        dao.insertBank(draft.toEntity(id, clock.nowEpochMillis()))
        return id
    }

    override suspend fun deleteBank(id: QuestionBankId) {
        dao.deleteBank(id.value)
    }

    override suspend fun addQuestion(draft: BankQuestionDraft): BankQuestionId {
        val id = BankQuestionId(uuidGenerator.newUuid())
        val now = clock.nowEpochMillis()
        database.withTransaction {
            val bank = requireNotNull(dao.getBank(draft.bankId.value)) {
                "question_bank_not_found"
            }
            dao.insertQuestion(draft.toEntity(id, now))
            dao.updateBank(
                bank.copy(
                    importStatus = QuestionBankImportStatus.IMPORTED.name,
                    updatedAt = now,
                ),
            )
        }
        return id
    }

    override suspend fun addQuestions(drafts: List<BankQuestionDraft>): List<BankQuestionId> {
        if (drafts.isEmpty()) return emptyList()
        val bankId = drafts.first().bankId
        require(drafts.all { it.bankId == bankId }) { "question_bank_mismatch" }
        val ids = drafts.map { BankQuestionId(uuidGenerator.newUuid()) }
        val now = clock.nowEpochMillis()
        database.withTransaction {
            val bank = requireNotNull(dao.getBank(bankId.value)) {
                "question_bank_not_found"
            }
            dao.insertQuestions(
                drafts.mapIndexed { index, draft ->
                    draft.toEntity(ids[index], now + index)
                },
            )
            dao.updateBank(
                bank.copy(
                    importStatus = QuestionBankImportStatus.IMPORTED.name,
                    updatedAt = now + drafts.lastIndex,
                ),
            )
        }
        return ids
    }

    override suspend fun createExam(
        bankId: QuestionBankId,
        questionCount: Int,
    ): ExamSessionId {
        require(questionCount > 0) { "question_count_invalid" }
        val now = clock.nowEpochMillis()
        val seed = now
        val sessionId = ExamSessionId(uuidGenerator.newUuid())
        database.withTransaction {
            val bank = requireNotNull(dao.getBank(bankId.value)) {
                "question_bank_not_found"
            }
            val questions = dao.getQuestions(bankId.value)
            require(questions.size >= questionCount) { "question_count_not_enough" }
            val selected = questions.shuffled(Random(seed)).take(questionCount)
            dao.insertSession(
                examSessionEntity(
                    id = sessionId,
                    bankId = bankId,
                    title = "${bank.name} 随机考卷",
                    seed = seed,
                    now = now,
                ),
            )
            dao.insertAttempts(
                selected.mapIndexed { index, question ->
                    examAttemptEntity(
                        id = ExamAttemptId(uuidGenerator.newUuid()),
                        sessionId = sessionId,
                        questionId = BankQuestionId(question.id),
                        orderIndex = index,
                    )
                },
            )
        }
        return sessionId
    }

    override suspend fun renameExam(sessionId: ExamSessionId, title: String) {
        val cleanedTitle = title.trim()
        require(cleanedTitle.isNotBlank()) { "exam_title_blank" }
        database.withTransaction {
            val session = requireNotNull(dao.getSession(sessionId.value)) {
                "exam_session_not_found"
            }
            dao.updateSession(session.copy(title = cleanedTitle))
        }
    }

    override suspend fun deleteExam(sessionId: ExamSessionId) {
        dao.deleteSession(sessionId.value)
    }

    override suspend fun submitAnswer(
        attemptId: ExamAttemptId,
        userAnswer: String,
    ) {
        val now = clock.nowEpochMillis()
        database.withTransaction {
            val attempt = requireNotNull(dao.getAttempt(attemptId.value)) {
                "exam_attempt_not_found"
            }
            val question = requireNotNull(dao.getQuestion(attempt.questionId)) {
                "bank_question_not_found"
            }
            val grade = gradeLocally(question, userAnswer)
            dao.updateAttempt(
                attempt.copy(
                    userAnswer = userAnswer.trim(),
                    answerRevealed = true,
                    gradingResult = grade.result.name,
                    gradingSource = grade.source.name,
                    gradingFeedback = grade.feedback,
                    submittedAt = now,
                ),
            )
        }
    }

    override suspend fun completeExam(sessionId: ExamSessionId) {
        val now = clock.nowEpochMillis()
        database.withTransaction {
            val session = requireNotNull(dao.getSession(sessionId.value)) {
                "exam_session_not_found"
            }
            require(dao.countUnrevealedAttempts(sessionId.value) == 0) {
                "exam_not_complete"
            }
            if (session.status != ExamSessionStatus.COMPLETED.name) {
                dao.updateSession(
                    session.copy(
                        status = ExamSessionStatus.COMPLETED.name,
                        completedAt = now,
                    ),
                )
            }
        }
    }

    private fun gradeLocally(
        question: BankQuestionEntity,
        userAnswer: String,
    ): LocalGrade {
        val expected = question.answer?.trim().orEmpty()
        if (expected.isBlank()) {
            return LocalGrade(
                result = ExamGradingResult.NOT_GRADED,
                source = ExamGradingSource.NONE,
                feedback = "暂无标准答案，已保留你的作答。",
            )
        }
        val type = enumValues<BankQuestionType>().firstOrNull { it.name == question.questionType }
            ?: BankQuestionType.UNKNOWN
        val correct = when (type) {
            BankQuestionType.MULTIPLE_CHOICE ->
                expected.toOptionSet() == userAnswer.toOptionSet()
            BankQuestionType.SINGLE_CHOICE ->
                expected.toOptionSet() == userAnswer.toOptionSet()
            BankQuestionType.FILL_BLANK,
            BankQuestionType.UNKNOWN,
            -> expected.normalizedAnswer() == userAnswer.normalizedAnswer()
            BankQuestionType.SUBJECTIVE -> null
        }
        return when (correct) {
            true -> LocalGrade(
                result = ExamGradingResult.CORRECT,
                source = ExamGradingSource.LOCAL,
                feedback = "答案匹配标准答案。",
            )
            false -> LocalGrade(
                result = ExamGradingResult.INCORRECT,
                source = ExamGradingSource.LOCAL,
                feedback = "答案与标准答案不一致。",
            )
            null -> LocalGrade(
                result = ExamGradingResult.NEEDS_REVIEW,
                source = ExamGradingSource.NONE,
                feedback = "主观题需要 AI 批改或人工复核。",
            )
        }
    }

    private data class LocalGrade(
        val result: ExamGradingResult,
        val source: ExamGradingSource,
        val feedback: String,
    )
}

private fun String.normalizedAnswer(): String =
    trim()
        .uppercase()
        .replace(Regex("\\s+"), "")
        .replace("，", ",")
        .replace("。", ".")

private fun String.toOptionSet(): Set<String> =
    uppercase()
        .split(Regex("[,，、\\s]+"))
        .flatMap { token ->
            val trimmed = token.trim()
            when {
                trimmed.matches(Regex("[A-Z]{2,}")) -> trimmed.map { it.toString() }
                else -> listOfNotNull(
                    Regex("""^([A-Z])(?:[.．):：].*)?$""")
                        .find(trimmed)
                        ?.groupValues
                        ?.getOrNull(1)
                        ?: trimmed.takeIf { it.matches(Regex("[A-Z]")) },
                )
            }
        }
        .filter { it.isNotBlank() }
        .toSet()
