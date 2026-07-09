package com.bandu.tiji.feature.questionbank

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bandu.tiji.core.model.enums.ExerciseDifficulty
import com.bandu.tiji.core.model.id.ExamAttemptId
import com.bandu.tiji.core.model.id.ExamSessionId
import com.bandu.tiji.core.model.id.QuestionBankId
import com.bandu.tiji.core.model.questionbank.BankQuestionDraft
import com.bandu.tiji.core.model.questionbank.QuestionBankDraft
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
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(QuestionBankUiState())
    val uiState: StateFlow<QuestionBankUiState> = mutableUiState.asStateFlow()

    private val mutableEffects = Channel<QuestionBankEffect>(Channel.BUFFERED)
    val effects = mutableEffects.receiveAsFlow()

    private var banksJob: Job? = null
    private var bankJob: Job? = null
    private var examJob: Job? = null

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
                mutableUiState.value = mutableUiState.value.copy(answerInput = action.answer)
            QuestionBankAction.SubmitCurrentAnswer -> submitCurrentAnswer()
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
                    mutableUiState.value = mutableUiState.value.copy(
                        banks = banks,
                        isLoading = false,
                        errorMessage = null,
                    )
                }
        }
    }

    private fun openBank(id: QuestionBankId) {
        bankJob?.cancel()
        examJob?.cancel()
        mutableUiState.value = mutableUiState.value.copy(
            screen = QuestionBankScreen.Detail(id),
            currentBank = null,
            currentExam = null,
            currentAttemptIndex = 0,
            answerInput = "",
            isLoading = true,
            errorMessage = null,
            noticeMessage = null,
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
                        errorMessage = if (bank == null) "题库不存在" else null,
                    )
                }
        }
    }

    private fun openExam(id: ExamSessionId) {
        examJob?.cancel()
        mutableUiState.value = mutableUiState.value.copy(
            screen = QuestionBankScreen.Exam(id),
            currentExam = null,
            currentAttemptIndex = 0,
            answerInput = "",
            isLoading = true,
            errorMessage = null,
            noticeMessage = null,
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
                        answerInput = current?.userAnswer.orEmpty(),
                        errorMessage = if (session == null) "考卷不存在" else null,
                    )
                }
        }
    }

    private fun importPdf(uri: String) {
        viewModelScope.launch {
            mutableUiState.value = mutableUiState.value.copy(
                isLoading = true,
                errorMessage = null,
                noticeMessage = null,
            )
            runCatching {
                val fileName = uri.toPdfFileName()
                repository.createBank(
                    QuestionBankDraft(
                        name = fileName.removeSuffix(".pdf").ifBlank { "PDF 题库" },
                        sourceFileName = fileName,
                        sourceUri = uri,
                    ),
                )
            }.onSuccess { id ->
                mutableUiState.value = mutableUiState.value.copy(
                    noticeMessage = "PDF 已导入为题库草稿，请手动添加或复核题目。",
                )
                openBank(id)
            }.onFailure {
                mutableUiState.value = mutableUiState.value.copy(
                    isLoading = false,
                    errorMessage = "无法导入 PDF，请重新选择文件。",
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
            mutableUiState.value = mutableUiState.value.copy(errorMessage = "请先填写答案")
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
                mutableUiState.value = mutableUiState.value.copy(
                    screen = QuestionBankScreen.List,
                    currentBank = null,
                    questionEditor = null,
                    examDialog = null,
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
