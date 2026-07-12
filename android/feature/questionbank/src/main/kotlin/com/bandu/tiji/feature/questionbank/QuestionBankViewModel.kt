package com.bandu.tiji.feature.questionbank

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bandu.tiji.core.model.enums.ExerciseDifficulty
import com.bandu.tiji.core.model.id.ExamAttemptId
import com.bandu.tiji.core.model.id.ExamSessionId
import com.bandu.tiji.core.model.id.QuestionBankId
import com.bandu.tiji.core.model.questionbank.BankQuestionDraft
import com.bandu.tiji.core.model.questionbank.ExamGradingResult
import com.bandu.tiji.core.model.questionbank.ExamSessionSummary
import com.bandu.tiji.core.model.questionbank.QuestionReviewStatus
import com.bandu.tiji.core.model.questionbank.QuestionBankDraft
import com.bandu.tiji.core.model.questionbank.QuestionBankSummary
import com.bandu.tiji.domain.ai.AiGatewayException
import com.bandu.tiji.domain.ai.SplitQuestionBankQuestion
import com.bandu.tiji.domain.ai.SplitQuestionBankPageRequest
import com.bandu.tiji.domain.repository.AiTutorGateway
import com.bandu.tiji.domain.repository.QuestionBankRepository
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class QuestionBankViewModel(
    private val repository: QuestionBankRepository,
    private val aiGateway: AiTutorGateway? = null,
    private val pdfPageExtractor: PdfQuestionBankPageExtractor? = null,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(QuestionBankUiState())
    val uiState: StateFlow<QuestionBankUiState> = mutableUiState.asStateFlow()

    private val mutableEffects = Channel<QuestionBankEffect>(Channel.BUFFERED)
    val effects = mutableEffects.receiveAsFlow()

    private var banksJob: Job? = null
    private var bankJob: Job? = null
    private var examSessionsJob: Job? = null
    private var examJob: Job? = null
    private var isPdfImportRunning = false

    init {
        observeBanks()
    }

    fun onAction(action: QuestionBankAction) {
        when (action) {
            QuestionBankAction.NavigateBack -> navigateBack()
            QuestionBankAction.Retry -> retry()
            QuestionBankAction.RequestPdfImport ->
                mutableEffects.trySend(QuestionBankEffect.LaunchPdfPicker)
            is QuestionBankAction.PdfSelected -> importPdf(action.uri)
            is QuestionBankAction.OpenBank -> openBank(action.id)
            is QuestionBankAction.OpenExam -> openExam(action.id)
            is QuestionBankAction.RequestDeleteBank -> requestDeleteBank(action.id)
            QuestionBankAction.DismissDeleteBank ->
                mutableUiState.value = mutableUiState.value.copy(pendingDeleteBank = null)
            QuestionBankAction.ConfirmDeleteBank -> deleteBank()
            is QuestionBankAction.RequestRenameExam -> requestRenameExam(action.id)
            is QuestionBankAction.UpdateRenameExam ->
                mutableUiState.value = mutableUiState.value.copy(renameExamDialog = action.dialog)
            QuestionBankAction.ConfirmRenameExam -> renameExam()
            QuestionBankAction.DismissRenameExam ->
                mutableUiState.value = mutableUiState.value.copy(renameExamDialog = null)
            is QuestionBankAction.RequestDeleteExam -> requestDeleteExam(action.id)
            QuestionBankAction.ConfirmDeleteExam -> deleteExam()
            QuestionBankAction.DismissDeleteExam ->
                mutableUiState.value = mutableUiState.value.copy(pendingDeleteExam = null)
            QuestionBankAction.OpenQuestionEditor -> {
                mutableUiState.value = mutableUiState.value.copy(
                    questionEditor = QuestionEditorUiState(),
                    errorMessage = null,
                )
            }
            QuestionBankAction.DismissQuestionEditor ->
                mutableUiState.value = mutableUiState.value.copy(questionEditor = null)
            is QuestionBankAction.UpdateQuestionEditor ->
                mutableUiState.value = mutableUiState.value.copy(questionEditor = action.editor)
            QuestionBankAction.SaveQuestion -> saveQuestion()
            QuestionBankAction.OpenGenerateExam ->
                mutableUiState.value = mutableUiState.value.copy(
                    examDialog = GenerateExamUiState(
                        questionCountText = defaultExamCount().toString(),
                    ),
                )
            QuestionBankAction.DismissGenerateExam ->
                mutableUiState.value = mutableUiState.value.copy(examDialog = null)
            is QuestionBankAction.UpdateGenerateExam ->
                mutableUiState.value = mutableUiState.value.copy(examDialog = action.dialog)
            QuestionBankAction.CreateExam -> createExam()
            is QuestionBankAction.UpdateAnswer ->
                mutableUiState.value = mutableUiState.value.copy(
                    answerInput = action.answer,
                    noticeMessage = null,
                )
            QuestionBankAction.SubmitCurrentAnswer -> submitCurrentAnswer()
            QuestionBankAction.SubmitExam -> submitExam()
            QuestionBankAction.NextAttempt -> moveToNextAttempt()
        }
    }

    private fun observeBanks() {
        banksJob?.cancel()
        banksJob = viewModelScope.launch {
            repository.observeBanks()
                .catch {
                    mutableUiState.value = mutableUiState.value.copy(
                        isLoading = false,
                        errorMessage = "无法加载 PDF 题库",
                    )
                }
                .collect { banks ->
                    val current = mutableUiState.value
                    mutableUiState.value = if (isPdfImportRunning) {
                        current.copy(banks = banks)
                    } else {
                        current.copy(
                            banks = banks,
                            isLoading = false,
                            loadingMessage = "正在加载题库",
                            errorMessage = null,
                        )
                    }
                }
        }
    }

    private fun openBank(
        id: QuestionBankId,
        noticeMessage: String? = null,
    ) {
        bankJob?.cancel()
        examSessionsJob?.cancel()
        examJob?.cancel()
        mutableUiState.value = mutableUiState.value.copy(
            screen = QuestionBankScreen.Detail(id),
            currentBank = null,
            currentExam = null,
            examSessions = emptyList(),
            currentAttemptIndex = 0,
            answerInput = "",
            isLoading = true,
            loadingMessage = "正在加载题库详情",
            errorMessage = null,
            noticeMessage = noticeMessage,
            renameExamDialog = null,
            pendingDeleteExam = null,
        )
        bankJob = viewModelScope.launch {
            repository.observeBank(id)
                .catch {
                    mutableUiState.value = mutableUiState.value.copy(
                        isLoading = false,
                        errorMessage = "无法加载题库详情",
                    )
                }
                .collect { bank ->
                    mutableUiState.value = mutableUiState.value.copy(
                        currentBank = bank,
                        isLoading = false,
                        loadingMessage = "正在加载题库",
                        errorMessage = if (bank == null) "题库不存在" else null,
                    )
                }
        }
        examSessionsJob = viewModelScope.launch {
            repository.observeExamSessions(id)
                .catch {
                    mutableUiState.value = mutableUiState.value.copy(examSessions = emptyList())
                }
                .collect { sessions ->
                    mutableUiState.value = mutableUiState.value.copy(examSessions = sessions)
                }
        }
    }

    private fun openExam(id: ExamSessionId) {
        examJob?.cancel()
        examSessionsJob?.cancel()
        mutableUiState.value = mutableUiState.value.copy(
            screen = QuestionBankScreen.Exam(id),
            currentExam = null,
            currentAttemptIndex = 0,
            answerInput = "",
            isLoading = true,
            loadingMessage = "正在加载考卷",
            errorMessage = null,
            noticeMessage = null,
            renameExamDialog = null,
            pendingDeleteExam = null,
        )
        examJob = viewModelScope.launch {
            repository.observeExamSession(id)
                .catch {
                    mutableUiState.value = mutableUiState.value.copy(
                        isLoading = false,
                        errorMessage = "无法加载考卷",
                    )
                }
                .collect { session ->
                    val current = session?.attempts?.getOrNull(mutableUiState.value.currentAttemptIndex)
                    mutableUiState.value = mutableUiState.value.copy(
                        currentExam = session,
                        isLoading = false,
                        loadingMessage = "正在加载题库",
                        answerInput = current?.userAnswer.orEmpty(),
                        errorMessage = if (session == null) "考卷不存在" else null,
                    )
                }
        }
    }

    private fun requestDeleteBank(id: QuestionBankId) {
        val bank = mutableUiState.value.banks.firstOrNull { it.id == id }
            ?: mutableUiState.value.currentBank?.takeIf { it.id == id }?.let {
                QuestionBankSummary(
                    id = it.id,
                    name = it.name,
                    sourceFileName = it.sourceFileName,
                    sourceUri = it.sourceUri,
                    subject = it.subject,
                    questionCount = it.questions.size,
                    importStatus = it.importStatus,
                    createdAtEpochMillis = it.createdAtEpochMillis,
                    updatedAtEpochMillis = it.updatedAtEpochMillis,
                )
            }
            ?: return
        mutableUiState.value = mutableUiState.value.copy(
            pendingDeleteBank = DeleteQuestionBankUiState(
                bankId = bank.id,
                bankName = bank.name,
                sourceFileName = bank.sourceFileName,
            ),
            errorMessage = null,
        )
    }

    private fun deleteBank() {
        val pending = mutableUiState.value.pendingDeleteBank ?: return
        viewModelScope.launch {
            mutableUiState.value = mutableUiState.value.copy(
                pendingDeleteBank = pending.copy(isDeleting = true, errorMessage = null),
            )
            runCatching {
                repository.deleteBank(pending.bankId)
            }.onSuccess {
                if (mutableUiState.value.currentBank?.id == pending.bankId) {
                    bankJob?.cancel()
                    examSessionsJob?.cancel()
                }
                mutableUiState.value = mutableUiState.value.copy(
                    screen = if (mutableUiState.value.currentBank?.id == pending.bankId) {
                        QuestionBankScreen.List
                    } else {
                        mutableUiState.value.screen
                    },
                    currentBank = mutableUiState.value.currentBank?.takeIf { it.id != pending.bankId },
                    examSessions = if (mutableUiState.value.currentBank?.id == pending.bankId) {
                        emptyList()
                    } else {
                        mutableUiState.value.examSessions
                    },
                    pendingDeleteBank = null,
                    isLoading = false,
                    noticeMessage = "题库已删除：${pending.bankName}",
                    errorMessage = null,
                )
            }.onFailure {
                mutableUiState.value = mutableUiState.value.copy(
                    pendingDeleteBank = pending.copy(
                        isDeleting = false,
                        errorMessage = "删除失败，请稍后重试",
                    ),
                )
            }
        }
    }

    private fun requestRenameExam(id: ExamSessionId) {
        val session = findExamSummary(id) ?: return
        mutableUiState.value = mutableUiState.value.copy(
            renameExamDialog = RenameExamUiState(
                sessionId = session.id,
                titleText = session.title,
            ),
            errorMessage = null,
        )
    }

    private fun renameExam() {
        val dialog = mutableUiState.value.renameExamDialog ?: return
        val title = dialog.titleText.trim()
        if (title.isBlank()) {
            mutableUiState.value = mutableUiState.value.copy(
                renameExamDialog = dialog.copy(errorMessage = "考卷名称不能为空"),
            )
            return
        }
        viewModelScope.launch {
            mutableUiState.value = mutableUiState.value.copy(
                renameExamDialog = dialog.copy(isSaving = true, errorMessage = null),
            )
            runCatching {
                repository.renameExam(dialog.sessionId, title)
            }.onSuccess {
                mutableUiState.value = mutableUiState.value.copy(
                    renameExamDialog = null,
                    noticeMessage = "考卷已重命名",
                    errorMessage = null,
                )
            }.onFailure {
                mutableUiState.value = mutableUiState.value.copy(
                    renameExamDialog = dialog.copy(
                        isSaving = false,
                        errorMessage = "重命名失败，请稍后重试",
                    ),
                )
            }
        }
    }

    private fun requestDeleteExam(id: ExamSessionId) {
        val session = findExamSummary(id) ?: return
        mutableUiState.value = mutableUiState.value.copy(
            pendingDeleteExam = DeleteExamUiState(
                sessionId = session.id,
                title = session.title,
            ),
            errorMessage = null,
        )
    }

    private fun deleteExam() {
        val pending = mutableUiState.value.pendingDeleteExam ?: return
        viewModelScope.launch {
            val currentBeforeDelete = mutableUiState.value.currentExam
            mutableUiState.value = mutableUiState.value.copy(
                pendingDeleteExam = pending.copy(isDeleting = true, errorMessage = null),
            )
            runCatching {
                repository.deleteExam(pending.sessionId)
            }.onSuccess {
                val current = currentBeforeDelete ?: mutableUiState.value.currentExam
                if (current?.id == pending.sessionId) {
                    examJob?.cancel()
                    openBank(current.bankId, "考卷已删除：${pending.title}")
                } else {
                    mutableUiState.value = mutableUiState.value.copy(
                        pendingDeleteExam = null,
                        noticeMessage = "考卷已删除：${pending.title}",
                        errorMessage = null,
                    )
                }
            }.onFailure {
                mutableUiState.value = mutableUiState.value.copy(
                    pendingDeleteExam = pending.copy(
                        isDeleting = false,
                        errorMessage = "删除失败，请稍后重试",
                    ),
                )
            }
        }
    }

    private fun findExamSummary(id: ExamSessionId): ExamSessionSummary? {
        val state = mutableUiState.value
        return state.examSessions.firstOrNull { it.id == id }
            ?: state.currentExam?.takeIf { it.id == id }?.let { session ->
                ExamSessionSummary(
                    id = session.id,
                    bankId = session.bankId,
                    title = session.title,
                    questionCount = session.attempts.size,
                    status = session.status,
                    createdAtEpochMillis = session.createdAtEpochMillis,
                    completedAtEpochMillis = session.completedAtEpochMillis,
                )
            }
    }

    private fun importPdf(uri: String) {
        if (isPdfImportRunning) return
        viewModelScope.launch {
            isPdfImportRunning = true
            mutableUiState.value = mutableUiState.value.copy(
                isLoading = true,
                isPdfImporting = true,
                loadingMessage = "正在准备 PDF 导入",
                errorMessage = null,
                noticeMessage = null,
            )
            runCatching {
                val extractor = pdfPageExtractor ?: throw PdfImportFailure.MissingPdfReader
                val gateway = aiGateway ?: throw PdfImportFailure.MissingAiConfiguration
                val fileName = uri.toPdfFileName()
                mutableUiState.value = mutableUiState.value.copy(
                    loadingMessage = "正在渲染 PDF 页面",
                )
                val document = extractor.extract(uri)
                if (document.pages.isEmpty()) throw PdfImportFailure.EmptyPdf

                val importedQuestions = mutableListOf<ImportedBankQuestion>()
                document.pages.forEachIndexed { index, page ->
                    mutableUiState.value = mutableUiState.value.copy(
                        loadingMessage = "正在拆题 ${index + 1}/${document.pages.size}",
                    )
                    val split = gateway.splitQuestionBankPage(
                        SplitQuestionBankPageRequest(
                            sourceFileName = fileName,
                            pageNumber = page.pageNumber,
                            pageImageBytes = page.imageBytes,
                            mimeType = page.mimeType,
                            languageInstruction = "请使用简体中文返回题目、答案、解析和标签。",
                        ),
                    )
                    importedQuestions += split.questions.map { question ->
                        question.toImportedBankQuestion(page.pageNumber)
                    }
                }
                if (importedQuestions.isEmpty()) throw PdfImportFailure.NoQuestions

                mutableUiState.value = mutableUiState.value.copy(
                    loadingMessage = "正在保存题库",
                )
                val bankId = repository.createBank(
                    QuestionBankDraft(
                        name = fileName.removeSuffix(".pdf").ifBlank { "PDF 题库" },
                        sourceFileName = fileName,
                        sourceUri = uri,
                    ),
                )
                repository.addQuestions(
                    importedQuestions.map { question ->
                        question.toDraft(bankId)
                    },
                )
                PdfImportResult(
                    bankId = bankId,
                    questionCount = importedQuestions.size,
                    importedPageCount = document.pages.size,
                    totalPageCount = document.totalPageCount,
                )
            }.onSuccess { id ->
                isPdfImportRunning = false
                mutableUiState.value = mutableUiState.value.copy(isPdfImporting = false)
                openBank(id.bankId, id.successMessage())
            }.onFailure { throwable ->
                isPdfImportRunning = false
                mutableUiState.value = mutableUiState.value.copy(
                    isLoading = false,
                    isPdfImporting = false,
                    loadingMessage = "正在加载题库",
                    errorMessage = throwable.toPdfImportErrorMessage(),
                )
            }
        }
    }

    private fun saveQuestion() {
        val bank = mutableUiState.value.currentBank ?: return
        val editor = mutableUiState.value.questionEditor ?: return
        val page = editor.sourcePageText.trim().ifEmpty { null }?.toIntOrNull()
        if (editor.stem.isBlank()) {
            mutableUiState.value = mutableUiState.value.copy(
                questionEditor = editor.copy(errorMessage = "题干不能为空"),
            )
            return
        }
        if (editor.sourcePageText.isNotBlank() && page == null) {
            mutableUiState.value = mutableUiState.value.copy(
                questionEditor = editor.copy(errorMessage = "来源页码必须是数字"),
            )
            return
        }
        viewModelScope.launch {
            mutableUiState.value = mutableUiState.value.copy(
                questionEditor = editor.copy(isSaving = true, errorMessage = null),
            )
            runCatching {
                repository.addQuestion(
                    BankQuestionDraft(
                        bankId = bank.id,
                        stem = editor.stem,
                        options = editor.optionsText.lines().map { it.trim() }.filter { it.isNotBlank() },
                        answer = editor.answer,
                        analysis = editor.analysis,
                        questionType = editor.questionType,
                        difficulty = editor.difficulty,
                        tags = editor.tagsText.split(Regex("[,，\\n]+"))
                            .map { it.trim() }
                            .filter { it.isNotBlank() },
                        sourcePage = page,
                    ),
                )
            }.onSuccess {
                mutableUiState.value = mutableUiState.value.copy(
                    questionEditor = null,
                    noticeMessage = "题目已添加",
                )
            }.onFailure {
                mutableUiState.value = mutableUiState.value.copy(
                    questionEditor = editor.copy(
                        isSaving = false,
                        errorMessage = "无法保存题目，请检查内容后重试",
                    ),
                )
            }
        }
    }

    private fun createExam() {
        val bank = mutableUiState.value.currentBank ?: return
        val dialog = mutableUiState.value.examDialog ?: return
        val count = dialog.questionCountText.trim().toIntOrNull()
        if (count == null || count <= 0) {
            mutableUiState.value = mutableUiState.value.copy(
                examDialog = dialog.copy(errorMessage = "题数必须大于 0"),
            )
            return
        }
        viewModelScope.launch {
            mutableUiState.value = mutableUiState.value.copy(
                examDialog = dialog.copy(isCreating = true, errorMessage = null),
            )
            runCatching {
                repository.createExam(bank.id, count)
            }.onSuccess { sessionId ->
                mutableUiState.value = mutableUiState.value.copy(examDialog = null)
                openExam(sessionId)
            }.onFailure { throwable ->
                mutableUiState.value = mutableUiState.value.copy(
                    examDialog = dialog.copy(
                        isCreating = false,
                        errorMessage = throwable.toExamErrorMessage(),
                    ),
                )
            }
        }
    }

    private fun submitCurrentAnswer() {
        val session = mutableUiState.value.currentExam ?: return
        val attempt = session.attempts.getOrNull(mutableUiState.value.currentAttemptIndex) ?: return
        val answer = mutableUiState.value.answerInput
        if (answer.isBlank()) {
            mutableUiState.value = mutableUiState.value.copy(noticeMessage = "请先选择或填写答案")
            return
        }
        viewModelScope.launch {
            runCatching {
                repository.submitAnswer(ExamAttemptId(attempt.id.value), answer)
            }.onFailure {
                mutableUiState.value = mutableUiState.value.copy(errorMessage = "无法提交答案，请重试")
            }
        }
    }

    private fun submitExam() {
        val session = mutableUiState.value.currentExam ?: return
        if (session.attempts.any { !it.answerRevealed }) {
            mutableUiState.value = mutableUiState.value.copy(noticeMessage = "请先完成所有题目")
            return
        }
        viewModelScope.launch {
            runCatching {
                repository.completeExam(session.id)
            }.onSuccess {
                val correct = session.attempts.count { it.gradingResult == ExamGradingResult.CORRECT }
                mutableUiState.value = mutableUiState.value.copy(
                    noticeMessage = "考卷已提交，得分 $correct/${session.attempts.size}",
                    errorMessage = null,
                )
            }.onFailure {
                mutableUiState.value = mutableUiState.value.copy(errorMessage = "无法提交考卷，请重试")
            }
        }
    }

    private fun moveToNextAttempt() {
        val session = mutableUiState.value.currentExam ?: return
        val next = (mutableUiState.value.currentAttemptIndex + 1)
            .coerceAtMost((session.attempts.size - 1).coerceAtLeast(0))
        val nextAttempt = session.attempts.getOrNull(next)
        mutableUiState.value = mutableUiState.value.copy(
            currentAttemptIndex = next,
            answerInput = nextAttempt?.userAnswer.orEmpty(),
            errorMessage = null,
        )
    }

    private fun navigateBack() {
        when (val screen = mutableUiState.value.screen) {
            QuestionBankScreen.List ->
                mutableEffects.trySend(QuestionBankEffect.NavigateBack)
            is QuestionBankScreen.Detail -> {
                bankJob?.cancel()
                examSessionsJob?.cancel()
                mutableUiState.value = mutableUiState.value.copy(
                    screen = QuestionBankScreen.List,
                    currentBank = null,
                    examSessions = emptyList(),
                    questionEditor = null,
                    examDialog = null,
                    pendingDeleteBank = null,
                    renameExamDialog = null,
                    pendingDeleteExam = null,
                    errorMessage = null,
                )
            }
            is QuestionBankScreen.Exam -> openBank(
                mutableUiState.value.currentExam?.bankId ?: return,
            )
        }
    }

    private fun retry() {
        when (val screen = mutableUiState.value.screen) {
            QuestionBankScreen.List -> observeBanks()
            is QuestionBankScreen.Detail -> openBank(screen.bankId)
            is QuestionBankScreen.Exam -> openExam(screen.sessionId)
        }
    }

    private fun defaultExamCount(): Int =
        mutableUiState.value.currentBank?.questions?.size?.coerceAtMost(5)?.coerceAtLeast(1) ?: 1

    private fun Throwable.toExamErrorMessage(): String =
        when (message) {
            "question_count_not_enough" -> "题库题数不足，无法生成考卷"
            "question_count_invalid" -> "题数必须大于 0"
            else -> "无法生成考卷，请稍后重试"
        }
}

private data class ImportedBankQuestion(
    val stem: String,
    val options: List<String>,
    val answer: String?,
    val analysis: String?,
    val questionType: com.bandu.tiji.core.model.questionbank.BankQuestionType,
    val difficulty: ExerciseDifficulty,
    val tags: List<String>,
    val sourcePage: Int,
    val sourceText: String?,
) {
    fun toDraft(bankId: QuestionBankId): BankQuestionDraft =
        BankQuestionDraft(
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
            reviewStatus = QuestionReviewStatus.NEEDS_REVIEW,
        )
}

private data class PdfImportResult(
    val bankId: QuestionBankId,
    val questionCount: Int,
    val importedPageCount: Int,
    val totalPageCount: Int,
) {
    fun successMessage(): String {
        val pageText = if (totalPageCount > importedPageCount) {
            "前 $importedPageCount/$totalPageCount 页"
        } else {
            "$importedPageCount 页"
        }
        return "PDF 已自动拆出 $questionCount 道题（$pageText），请复核后生成考卷。"
    }
}

private sealed class PdfImportFailure : RuntimeException() {
    data object MissingPdfReader : PdfImportFailure()
    data object MissingAiConfiguration : PdfImportFailure()
    data object EmptyPdf : PdfImportFailure()
    data object NoQuestions : PdfImportFailure()
}

private fun SplitQuestionBankQuestion.toImportedBankQuestion(sourcePage: Int): ImportedBankQuestion =
    ImportedBankQuestion(
        stem = stem.trim(),
        options = options.map { it.trim() }.filter { it.isNotBlank() },
        answer = answer?.trim()?.ifBlank { null },
        analysis = analysis?.trim()?.ifBlank { null },
        questionType = questionType,
        difficulty = difficulty,
        tags = tags.map { it.trim() }.filter { it.isNotBlank() },
        sourcePage = sourcePage,
        sourceText = sourceText?.trim()?.ifBlank { null } ?: stem.trim(),
    )

private fun Throwable.toPdfImportErrorMessage(): String =
    when (this) {
        PdfImportFailure.MissingPdfReader -> "当前版本无法读取 PDF 页面，请稍后重试。"
        PdfImportFailure.MissingAiConfiguration,
        AiGatewayException.ConfigurationRequired,
        -> "请先在“我的 - AI 配置”保存可用模型后再导入 PDF。"
        PdfImportFailure.EmptyPdf -> "PDF 没有可读取页面，请重新选择文件。"
        PdfImportFailure.NoQuestions -> "未能从 PDF 中拆出题目，请换用更清晰的 PDF 或手动添加题目。"
        AiGatewayException.Authentication -> "AI 密钥认证失败，请检查 API key 后重试。"
        AiGatewayException.RateLimited -> "AI 服务限流，请稍后再试。"
        AiGatewayException.Timeout -> "AI 拆题超时，请稍后重试或选择页数更少的 PDF。"
        AiGatewayException.NetworkUnavailable -> "网络不可用，无法调用 AI 拆题。"
        is AiGatewayException.EndpointRejected -> "AI 服务拒绝请求：$reason"
        is AiGatewayException.InvalidResponse -> "AI 返回格式无法解析，请重试。"
        else -> "无法导入 PDF，请重新选择文件。"
    }

private fun String.toPdfFileName(): String {
    val decoded = runCatching {
        URLDecoder.decode(this, StandardCharsets.UTF_8.name())
    }.getOrDefault(this)
    return decoded
        .substringBefore('?')
        .substringAfterLast('/')
        .ifBlank { "PDF 题库.pdf" }
        .let { if (it.endsWith(".pdf", ignoreCase = true)) it else "$it.pdf" }
}
