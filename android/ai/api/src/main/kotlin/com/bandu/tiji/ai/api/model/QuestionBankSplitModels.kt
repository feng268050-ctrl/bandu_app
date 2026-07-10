package com.bandu.tiji.ai.api.model

import com.bandu.tiji.core.model.enums.ExerciseDifficulty
import com.bandu.tiji.core.model.questionbank.BankQuestionType

data class SplitQuestionBankPageRequest(
    val sourceFileName: String,
    val pageNumber: Int,
    val pageImageBytes: ByteArray,
    val mimeType: String = "image/jpeg",
    val languageInstruction: String = "",
    val gradeInstruction: String = "",
    val providerHints: String = "",
) {
    init {
        require(sourceFileName.isNotBlank()) { "sourceFileName must not be blank" }
        require(pageNumber > 0) { "pageNumber must be positive" }
        require(pageImageBytes.isNotEmpty()) { "pageImageBytes must not be empty" }
        require(mimeType.isNotBlank()) { "mimeType must not be blank" }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SplitQuestionBankPageRequest) return false
        return sourceFileName == other.sourceFileName &&
            pageNumber == other.pageNumber &&
            pageImageBytes.contentEquals(other.pageImageBytes) &&
            mimeType == other.mimeType &&
            languageInstruction == other.languageInstruction &&
            gradeInstruction == other.gradeInstruction &&
            providerHints == other.providerHints
    }

    override fun hashCode(): Int {
        var result = sourceFileName.hashCode()
        result = 31 * result + pageNumber
        result = 31 * result + pageImageBytes.contentHashCode()
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + languageInstruction.hashCode()
        result = 31 * result + gradeInstruction.hashCode()
        result = 31 * result + providerHints.hashCode()
        return result
    }
}

data class SplitQuestionBankPage(
    val questions: List<SplitQuestionBankQuestion>,
)

data class SplitQuestionBankQuestion(
    val stem: String,
    val options: List<String> = emptyList(),
    val answer: String? = null,
    val analysis: String? = null,
    val questionType: BankQuestionType = BankQuestionType.UNKNOWN,
    val difficulty: ExerciseDifficulty = ExerciseDifficulty.MEDIUM,
    val tags: List<String> = emptyList(),
    val sourceText: String? = null,
) {
    init {
        require(stem.isNotBlank()) { "stem must not be blank" }
    }
}
