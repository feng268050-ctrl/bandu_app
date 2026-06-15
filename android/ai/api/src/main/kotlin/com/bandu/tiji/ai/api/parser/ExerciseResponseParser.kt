package com.bandu.tiji.ai.api.parser

import com.bandu.tiji.ai.api.error.AiError
import com.bandu.tiji.ai.api.model.ExerciseGrade
import com.bandu.tiji.ai.api.model.GeneratedExercise
import com.bandu.tiji.core.model.enums.GradeResult

class ExerciseResponseParser {
    fun parseGeneratedExercise(text: String): GeneratedExercise {
        val questionText = requireTag(text, "question_text")
        val answerText = requireTag(text, "answer_text")
        val analysis = requireTag(text, "analysis")

        if (questionText.isBlank() || answerText.isBlank() || analysis.isBlank()) {
            throw AiError.InvalidResponse("exercise.empty_required_field")
        }

        return GeneratedExercise(
            questionText = questionText,
            answerText = answerText,
            analysis = analysis,
        )
    }

    fun parseExerciseGrade(text: String): ExerciseGrade {
        val gradeRaw = requireTag(text, "grade")
        val feedback = requireTag(text, "feedback")
        val confidenceRaw = requireTag(text, "confidence")

        val rawResult = parseGrade(gradeRaw)
        val confidence = parseConfidence(confidenceRaw)
        val result = if (confidence < ExerciseGrade.LOW_CONFIDENCE_THRESHOLD) {
            GradeResult.NEEDS_REVIEW
        } else {
            rawResult
        }

        return ExerciseGrade(
            result = result,
            feedback = feedback,
            confidence = confidence,
            rawResult = rawResult,
        )
    }

    private fun requireTag(text: String, tagName: String): String {
        val value = TagExtractor.extractTag(text, tagName)
            ?: throw AiError.InvalidResponse("exercise.missing_tag:$tagName")
        return value
    }

    private fun parseGrade(raw: String): GradeResult = when (raw.lowercase().trim()) {
        "correct" -> GradeResult.CORRECT
        "incorrect" -> GradeResult.INCORRECT
        "needs_review" -> GradeResult.NEEDS_REVIEW
        else -> throw AiError.InvalidResponse("exercise.invalid_grade")
    }

    private fun parseConfidence(raw: String): Double {
        val value = raw.trim().toDoubleOrNull()
            ?: throw AiError.InvalidResponse("exercise.invalid_confidence")
        if (value !in 0.0..1.0) {
            throw AiError.InvalidResponse("exercise.invalid_confidence_range")
        }
        return value
    }
}
