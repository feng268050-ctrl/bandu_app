package com.bandu.tiji.feature.questionbank

import com.bandu.tiji.core.model.enums.ExerciseDifficulty
import com.bandu.tiji.core.model.id.ExamSessionId
import com.bandu.tiji.core.model.id.QuestionBankId
import com.bandu.tiji.core.model.questionbank.BankQuestionType
import com.bandu.tiji.core.model.questionbank.ExamSession
import com.bandu.tiji.core.model.questionbank.ExamSessionSummary
import com.bandu.tiji.core.model.questionbank.QuestionBank
import com.bandu.tiji.core.model.questionbank.QuestionBankSummary

data class QuestionBankUiState(
    val screen: QuestionBankScreen = QuestionBankScreen.List,
    val banks: List<QuestionBankSummary> = emptyList(),
    val currentBank: QuestionBank? = null,
    val currentExam: ExamSession? = null,
    val examSessions: List<ExamSessionSummary> = emptyList(),
    val currentAttemptIndex: Int = 0,
    val answerInput: String = "",
    val isLoading: Boolean = true,
    val isPdfImporting: Boolean = false,
    val loadingMessage: String = "正在加载题库",
    val errorMessage: String? = null,
    val noticeMessage: String? = null,
    val questionEditor: QuestionEditorUiState? = null,
    val examDialog: GenerateExamUiState? = null,
    val pendingDeleteBank: DeleteQuestionBankUiState? = null,
    val renameExamDialog: RenameExamUiState? = null,
    val pendingDeleteExam: DeleteExamUiState? = null,
)

sealed interface QuestionBankScreen {
    data object List : QuestionBankScreen

    data class Detail(val bankId: QuestionBankId) : QuestionBankScreen

    data class Exam(val sessionId: ExamSessionId) : QuestionBankScreen
}

data class QuestionEditorUiState(
    val stem: String = "",
    val optionsText: String = "",
    val answer: String = "",
    val analysis: String = "",
    val questionType: BankQuestionType = BankQuestionType.UNKNOWN,
    val difficulty: ExerciseDifficulty = ExerciseDifficulty.MEDIUM,
    val tagsText: String = "",
    val sourcePageText: String = "",
    val errorMessage: String? = null,
    val isSaving: Boolean = false,
)

data class GenerateExamUiState(
    val questionCountText: String = "5",
    val errorMessage: String? = null,
    val isCreating: Boolean = false,
)

data class DeleteQuestionBankUiState(
    val bankId: QuestionBankId,
    val bankName: String,
    val sourceFileName: String,
    val isDeleting: Boolean = false,
    val errorMessage: String? = null,
)

data class RenameExamUiState(
    val sessionId: ExamSessionId,
    val titleText: String,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
)

data class DeleteExamUiState(
    val sessionId: ExamSessionId,
    val title: String,
    val isDeleting: Boolean = false,
    val errorMessage: String? = null,
)

sealed interface QuestionBankAction {
    data object NavigateBack : QuestionBankAction

    data object Retry : QuestionBankAction

    data object RequestPdfImport : QuestionBankAction

    data class PdfSelected(val uri: String) : QuestionBankAction

    data class OpenBank(val id: QuestionBankId) : QuestionBankAction

    data class OpenExam(val id: ExamSessionId) : QuestionBankAction

    data class RequestDeleteBank(val id: QuestionBankId) : QuestionBankAction

    data object DismissDeleteBank : QuestionBankAction

    data object ConfirmDeleteBank : QuestionBankAction

    data class RequestRenameExam(val id: ExamSessionId) : QuestionBankAction

    data class UpdateRenameExam(val dialog: RenameExamUiState) : QuestionBankAction

    data object ConfirmRenameExam : QuestionBankAction

    data object DismissRenameExam : QuestionBankAction

    data class RequestDeleteExam(val id: ExamSessionId) : QuestionBankAction

    data object ConfirmDeleteExam : QuestionBankAction

    data object DismissDeleteExam : QuestionBankAction

    data object OpenQuestionEditor : QuestionBankAction

    data object DismissQuestionEditor : QuestionBankAction

    data class UpdateQuestionEditor(val editor: QuestionEditorUiState) : QuestionBankAction

    data object SaveQuestion : QuestionBankAction

    data object OpenGenerateExam : QuestionBankAction

    data object DismissGenerateExam : QuestionBankAction

    data class UpdateGenerateExam(val dialog: GenerateExamUiState) : QuestionBankAction

    data object CreateExam : QuestionBankAction

    data class UpdateAnswer(val answer: String) : QuestionBankAction

    data object SubmitCurrentAnswer : QuestionBankAction

    data object SubmitExam : QuestionBankAction

    data object NextAttempt : QuestionBankAction
}

sealed interface QuestionBankEffect {
    data object NavigateBack : QuestionBankEffect

    data object LaunchPdfPicker : QuestionBankEffect
}
