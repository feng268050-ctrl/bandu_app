package com.bandu.tiji.ai.api.parser

import com.bandu.tiji.ai.api.error.AiError
import com.bandu.tiji.ai.api.model.ExerciseGrade
import com.bandu.tiji.core.model.enums.GradeResult
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class ExerciseResponseParserTest {
    private val parser = ExerciseResponseParser()

    @Test
    fun `parses generated exercise tags`() {
        val response = """
            <question_text>新题</question_text>
            <answer_text>新答案</answer_text>
            <analysis>新解析</analysis>
        """.trimIndent()

        val result = parser.parseGeneratedExercise(response)
        assertThat(result.questionText).isEqualTo("新题")
        assertThat(result.answerText).isEqualTo("新答案")
        assertThat(result.analysis).isEqualTo("新解析")
    }

    @Test
    fun `parses grade with confidence threshold`() {
        val response = """
            <grade>correct</grade>
            <feedback>很好</feedback>
            <confidence>0.90</confidence>
        """.trimIndent()

        val result = parser.parseExerciseGrade(response)
        assertThat(result.rawResult).isEqualTo(GradeResult.CORRECT)
        assertThat(result.result).isEqualTo(GradeResult.CORRECT)
        assertThat(result.confidence).isWithin(0.001).of(0.90)
    }

    @Test
    fun `low confidence forces needs review`() {
        val response = """
            <grade>correct</grade>
            <feedback>不确定</feedback>
            <confidence>0.50</confidence>
        """.trimIndent()

        val result = parser.parseExerciseGrade(response)
        assertThat(result.rawResult).isEqualTo(GradeResult.CORRECT)
        assertThat(result.result).isEqualTo(GradeResult.NEEDS_REVIEW)
        assertThat(result.confidence).isLessThan(ExerciseGrade.LOW_CONFIDENCE_THRESHOLD)
    }

    @Test
    fun `invalid grade is rejected`() {
        val response = """
            <grade>maybe</grade>
            <feedback>反馈</feedback>
            <confidence>0.80</confidence>
        """.trimIndent()

        val error = assertThrows(AiError.InvalidResponse::class.java) {
            parser.parseExerciseGrade(response)
        }
        assertThat(error.diagnosticCode).isEqualTo("exercise.invalid_grade")
    }

    @Test
    fun `invalid confidence is rejected`() {
        val response = """
            <grade>correct</grade>
            <feedback>反馈</feedback>
            <confidence>1.5</confidence>
        """.trimIndent()

        val error = assertThrows(AiError.InvalidResponse::class.java) {
            parser.parseExerciseGrade(response)
        }
        assertThat(error.diagnosticCode).isEqualTo("exercise.invalid_confidence_range")
    }
}
