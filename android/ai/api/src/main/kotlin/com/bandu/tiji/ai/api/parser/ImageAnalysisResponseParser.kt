package com.bandu.tiji.ai.api.parser

import com.bandu.tiji.ai.api.error.AiError
import com.bandu.tiji.ai.api.model.AnalyzedQuestion
import com.bandu.tiji.core.model.enums.MistakeStatus

class ImageAnalysisResponseParser {
    fun parse(text: String): AnalyzedQuestion {
        val questionText = requireTag(text, "question_text")
        val answerText = requireTag(text, "answer_text")
        val analysis = requireTag(text, "analysis")
        val subjectRaw = requireTag(text, "subject")
        val knowledgePointsRaw = requireTag(text, "knowledge_points")
        val requiresImageRaw = requireTag(text, "requires_image")
        val wrongAnswerText = TagExtractor.extractTag(text, "wrong_answer_text").orEmpty()
        val mistakeAnalysis = TagExtractor.extractTag(text, "mistake_analysis").orEmpty()
        val mistakeStatusRaw = requireTag(text, "mistake_status")

        if (questionText.isBlank() || answerText.isBlank() || analysis.isBlank()) {
            throw AiError.InvalidResponse("analysis.empty_required_field")
        }

        val subject = if (subjectRaw in VALID_SUBJECTS) {
            subjectRaw
        } else {
            DEFAULT_SUBJECT
        }

        val knowledgePoints = parseKnowledgePoints(knowledgePointsRaw)
        if (knowledgePoints.size > AnalyzedQuestion.MAX_KNOWLEDGE_POINTS) {
            throw AiError.InvalidResponse("analysis.too_many_knowledge_points")
        }

        val requiresImage = requiresImageRaw.lowercase().trim() == "true"
        val mistakeStatus = normalizeMistakeStatus(mistakeStatusRaw, wrongAnswerText)

        return AnalyzedQuestion(
            subject = subject,
            knowledgePoints = knowledgePoints,
            requiresImage = requiresImage,
            wrongAnswerText = wrongAnswerText,
            mistakeStatus = mistakeStatus,
            mistakeAnalysis = mistakeAnalysis,
            questionText = questionText,
            answerText = answerText,
            analysis = analysis,
        )
    }

    private fun requireTag(text: String, tagName: String): String {
        val value = TagExtractor.extractTag(text, tagName)
            ?: throw AiError.InvalidResponse("analysis.missing_tag:$tagName")
        return value
    }

    private fun parseKnowledgePoints(raw: String): List<String> =
        raw.split(KNOWLEDGE_POINT_DELIMITER)
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    private fun normalizeMistakeStatus(raw: String, wrongAnswerText: String): MistakeStatus {
        if (wrongAnswerText.isNotBlank()) {
            return MistakeStatus.WRONG_ATTEMPT
        }
        return when (raw.lowercase().trim()) {
            "wrong_attempt" -> MistakeStatus.WRONG_ATTEMPT
            "not_attempted" -> MistakeStatus.NOT_ATTEMPTED
            "unknown" -> MistakeStatus.UNKNOWN
            else -> throw AiError.InvalidResponse("analysis.invalid_mistake_status")
        }
    }

    companion object {
        private val KNOWLEDGE_POINT_DELIMITER = Regex("""[,，\n]""")
        private const val DEFAULT_SUBJECT = "其他"

        val VALID_SUBJECTS = setOf(
            "数学",
            "物理",
            "化学",
            "生物",
            "英语",
            "语文",
            "历史",
            "地理",
            "政治",
            "其他",
        )
    }
}
