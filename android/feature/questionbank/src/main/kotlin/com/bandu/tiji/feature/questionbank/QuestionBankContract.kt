package com.bandu.tiji.feature.questionbank

import com.bandu.tiji.core.model.enums.ExerciseDifficulty
import com.bandu.tiji.core.model.id.QuestionBankId
import com.bandu.tiji.core.model.questionbank.BankQuestionType
import com.bandu.tiji.core.model.questionbank.ExamSession
import com.bandu.tiji.core.model.questionbank.QuestionBank
import com.bandu.tiji.core.model.questionbank.QuestionBankSummary

data class QuestionBankUiState(
    val screen: QuestionBankScreen = QuestionBankScreen.List,
    val banks: List<QuestionBankSummary> = emptyList(),
    val currentBank: QuestionBank? = null,
    val currentExam: ExamSession? = null,
    val currentAttemptIndex: Int = 0,
    val answerInput: String = "",
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val noticeMessage: String? = null,
    val questionEditor: QuestionEditorUiState? = null,
    val examDialog: GenerateExamUiState? = null,
)

sealed interface QuestionBankScreen {
    data object List : QuestionBankScreen

    data class Detail(val bankId: QuestionBankId) : QuestionBankScreen

    data class Exam(val sessionId: com.bandu.tiji.core.model.id.ExamSessionId) : QuestionBankScreen
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

sealed interface QuestionBankAction {
    data object NavigateBack : QuestionBankAction

    data object Retry : QuestionBankAction

    data object RequestPdfImport : QuestionBankAction

    data class PdfSelected(val uri: String) : QuestionBankAction

    data class OpenBank(val id: QuestionBankId) : QuestionBankAction

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

    data object NextAttempt : QuestionBankAction
}

sealed interface QuestionBankEffect {
    data object NavigateBack : QuestionBankEffect

    data object LaunchPdfPicker : QuestionBankEffect
}
