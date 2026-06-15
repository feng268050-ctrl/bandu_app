package com.bandu.tiji.ai.api.model

import com.bandu.tiji.core.model.enums.MistakeStatus

data class AnalyzedQuestion(
    val subject: String,
    val knowledgePoints: List<String>,
    val requiresImage: Boolean,
    val wrongAnswerText: String,
    val mistakeStatus: MistakeStatus,
    val mistakeAnalysis: String,
    val questionText: String,
    val answerText: String,
    val analysis: String,
) {
    init {
        require(subject.isNotBlank()) { "subject must not be blank" }
        require(questionText.isNotBlank()) { "questionText must not be blank" }
        require(answerText.isNotBlank()) { "answerText must not be blank" }
        require(analysis.isNotBlank()) { "analysis must not be blank" }
        require(knowledgePoints.size <= MAX_KNOWLEDGE_POINTS) {
            "At most $MAX_KNOWLEDGE_POINTS knowledge points are allowed"
        }
        knowledgePoints.forEach { point ->
            require(point.isNotBlank()) { "knowledge points must not contain blank entries" }
        }
    }

    companion object {
        const val MAX_KNOWLEDGE_POINTS = 5
    }
}
