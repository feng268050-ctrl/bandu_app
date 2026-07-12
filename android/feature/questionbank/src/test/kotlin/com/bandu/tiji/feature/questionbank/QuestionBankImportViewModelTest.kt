package com.bandu.tiji.feature.questionbank

import com.bandu.tiji.core.model.enums.ExerciseDifficulty
import com.bandu.tiji.core.model.id.BankQuestionId
import com.bandu.tiji.core.model.id.ExamAttemptId
import com.bandu.tiji.core.model.id.ExamSessionId
import com.bandu.tiji.core.model.id.QuestionBankId
import com.bandu.tiji.core.model.questionbank.BankQuestion
import com.bandu.tiji.core.model.questionbank.BankQuestionDraft
import com.bandu.tiji.core.model.questionbank.BankQuestionType
import com.bandu.tiji.core.model.questionbank.ExamSession
import com.bandu.tiji.core.model.questionbank.ExamSessionStatus
import com.bandu.tiji.core.model.questionbank.ExamSessionSummary
import com.bandu.tiji.core.model.questionbank.QuestionBank
import com.bandu.tiji.core.model.questionbank.QuestionBankDraft
import com.bandu.tiji.core.model.questionbank.QuestionBankImportStatus
import com.bandu.tiji.core.model.questionbank.QuestionBankSummary
import com.bandu.tiji.core.model.questionbank.QuestionReviewStatus
import com.bandu.tiji.core.testing.coroutines.MainDispatcherRule
import com.bandu.tiji.core.testing.fake.CallScript
import com.bandu.tiji.core.testing.fake.FakeAiTutorGateway
import com.bandu.tiji.domain.ai.SplitQuestionBankPage
import com.bandu.tiji.domain.ai.SplitQuestionBankQuestion
import com.bandu.tiji.domain.repository.QuestionBankRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class QuestionBankImportViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `pdf import renders pages splits questions and stores review drafts`() = runTest {
        val repository = FakeQuestionBankRepository()
        val extractor = FakePdfPageExtractor(
            PdfQuestionBankDocument(
                totalPageCount = 1,
                pages = listOf(PdfQuestionBankPage(1, byteArrayOf(1, 2, 3))),
            ),
        )
        val gateway = FakeAiTutorGateway().apply {
            enqueueSplitPage(
                CallScript.Return(
                    SplitQuestionBankPage(
                        listOf(
                            SplitQuestionBankQuestion(
                                stem = "1 + 1 = ?",
                                options = listOf("A. 1", "B. 2"),
                                answer = "B",
                                analysis = "基础加法",
                                questionType = BankQuestionType.SINGLE_CHOICE,
                                difficulty = ExerciseDifficulty.EASY,
                                tags = listOf("加法"),
                                sourceText = "原文",
                            ),
                        ),
                    ),
                ),
            )
        }
        val viewModel = QuestionBankViewModel(
            repository = repository,
            aiGateway = gateway,
            pdfPageExtractor = extractor,
        )
        advanceUntilIdle()

        viewModel.onAction(QuestionBankAction.PdfSelected("content://downloads/math.pdf"))
        advanceUntilIdle()

        assertThat(extractor.selectedUris).containsExactly("content://downloads/math.pdf")
        assertThat(gateway.splitPageRequests).hasSize(1)
        assertThat(gateway.splitPageRequests.single().sourceFileName).isEqualTo("math.pdf")
        assertThat(gateway.splitPageRequests.single().pageNumber).isEqualTo(1)
        assertThat(repository.createdBanks.single().sourceFileName).isEqualTo("math.pdf")
        val saved = repository.addedBatches.single().single()
        assertThat(saved.stem).isEqualTo("1 + 1 = ?")
        assertThat(saved.options).containsExactly("A. 1", "B. 2").inOrder()
        assertThat(saved.reviewStatus).isEqualTo(QuestionReviewStatus.NEEDS_REVIEW)
        assertThat(saved.sourcePage).isEqualTo(1)
        assertThat(viewModel.uiState.value.currentBank?.questions?.single()?.stem)
            .isEqualTo("1 + 1 = ?")
        assertThat(viewModel.uiState.value.noticeMessage).contains("PDF 已自动拆出 1 道题")
    }
}

private class FakePdfPageExtractor(
    private val document: PdfQuestionBankDocument,
) : PdfQuestionBankPageExtractor {
    val selectedUris = mutableListOf<String>()

    override suspend fun extract(uri: String): PdfQuestionBankDocument {
        selectedUris += uri
        return document
    }
}

private class FakeQuestionBankRepository : QuestionBankRepository {
    private val banks = MutableStateFlow<List<QuestionBankSummary>>(emptyList())
    private val bankDetails = mutableMapOf<QuestionBankId, MutableStateFlow<QuestionBank?>>()
    private val examSessions = mutableMapOf<QuestionBankId, MutableStateFlow<List<ExamSessionSummary>>>()
    private var nextBank = 1
    private var nextQuestion = 1

    val createdBanks = mutableListOf<QuestionBankDraft>()
    val addedBatches = mutableListOf<List<BankQuestionDraft>>()

    override fun observeBanks(): Flow<List<QuestionBankSummary>> = banks.asStateFlow()

    override fun observeBank(id: QuestionBankId): Flow<QuestionBank?> =
        bankDetails.getValue(id).asStateFlow()

    override fun observeExamSession(id: ExamSessionId): Flow<ExamSession?> =
        MutableStateFlow<ExamSession?>(null).asStateFlow()

    override fun observeExamSessions(bankId: QuestionBankId): Flow<List<ExamSessionSummary>> =
        examSessions.getOrPut(bankId) { MutableStateFlow(emptyList()) }.asStateFlow()

    override suspend fun createBank(draft: QuestionBankDraft): QuestionBankId {
        createdBanks += draft
        val id = QuestionBankId("bank-${nextBank++}")
        val bank = QuestionBank(
            id = id,
            name = draft.name,
            sourceFileName = draft.sourceFileName,
            sourceUri = draft.sourceUri,
            subject = draft.subject,
            importStatus = QuestionBankImportStatus.REVIEW_REQUIRED,
            questions = emptyList(),
            createdAtEpochMillis = 1L,
            updatedAtEpochMillis = 1L,
        )
        bankDetails[id] = MutableStateFlow(bank)
        examSessions[id] = MutableStateFlow(emptyList())
        banks.value = listOf(bank.toSummary())
        return id
    }

    override suspend fun deleteBank(id: QuestionBankId) {
        bankDetails.remove(id)
        examSessions.remove(id)
        banks.value = banks.value.filterNot { it.id == id }
    }

    override suspend fun addQuestion(draft: BankQuestionDraft): BankQuestionId =
        addQuestions(listOf(draft)).single()

    override suspend fun addQuestions(drafts: List<BankQuestionDraft>): List<BankQuestionId> {
        addedBatches += drafts
        val ids = drafts.map { BankQuestionId("question-${nextQuestion++}") }
        val bankId = drafts.first().bankId
        val bank = requireNotNull(bankDetails.getValue(bankId).value)
        val questions = drafts.mapIndexed { index, draft ->
            draft.toQuestion(ids[index])
        }
        val updated = bank.copy(
            importStatus = QuestionBankImportStatus.IMPORTED,
            questions = bank.questions + questions,
            updatedAtEpochMillis = 2L,
        )
        bankDetails.getValue(bankId).value = updated
        banks.value = listOf(updated.toSummary())
        return ids
    }

    override suspend fun createExam(
        bankId: QuestionBankId,
        questionCount: Int,
    ): ExamSessionId {
        val id = ExamSessionId("exam-1")
        val current = examSessions.getOrPut(bankId) { MutableStateFlow(emptyList()) }
        current.value = listOf(
            ExamSessionSummary(
                id = id,
                bankId = bankId,
                title = "测试考卷",
                questionCount = questionCount,
                status = ExamSessionStatus.IN_PROGRESS,
                createdAtEpochMillis = 3L,
                completedAtEpochMillis = null,
            ),
        )
        return id
    }

    override suspend fun renameExam(sessionId: ExamSessionId, title: String) {
        examSessions.values.forEach { sessions ->
            sessions.value = sessions.value.map { session ->
                if (session.id == sessionId) {
                    session.copy(title = title.trim())
                } else {
                    session
                }
            }
        }
    }

    override suspend fun deleteExam(sessionId: ExamSessionId) {
        examSessions.values.forEach { sessions ->
            sessions.value = sessions.value.filterNot { it.id == sessionId }
        }
    }

    override suspend fun submitAnswer(
        attemptId: ExamAttemptId,
        userAnswer: String,
    ) = Unit

    override suspend fun completeExam(sessionId: ExamSessionId) = Unit

    private fun BankQuestionDraft.toQuestion(id: BankQuestionId): BankQuestion =
        BankQuestion(
            id = id,
            bankId = bankId,
            stem = stem,
            options = options,
            answer = answer,
            analysis = analysis,
            questionType = questionType,
            difficulty = difficulty,
            tags = tags,
            sourcePage = sourcePage,
            sourceText = sourceText,
            sourceImageUri = sourceImageUri,
            reviewStatus = reviewStatus,
            createdAtEpochMillis = 2L,
            updatedAtEpochMillis = 2L,
        )

    private fun QuestionBank.toSummary(): QuestionBankSummary =
        QuestionBankSummary(
            id = id,
            name = name,
            sourceFileName = sourceFileName,
            sourceUri = sourceUri,
            subject = subject,
            questionCount = questions.size,
            importStatus = importStatus,
            createdAtEpochMillis = createdAtEpochMillis,
            updatedAtEpochMillis = updatedAtEpochMillis,
        )
}
