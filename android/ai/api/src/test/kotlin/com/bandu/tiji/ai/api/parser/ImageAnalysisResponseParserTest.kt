package com.bandu.tiji.ai.api.parser

import com.bandu.tiji.ai.api.error.AiError
import com.bandu.tiji.core.model.enums.MistakeStatus
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class ImageAnalysisResponseParserTest {
    private val parser = ImageAnalysisResponseParser()

    @Test
    fun `parses complete tagged response`() {
        val response = """
            <subject>数学</subject>
            <knowledge_points>二次函数, 函数最值</knowledge_points>
            <requires_image>true</requires_image>
            <wrong_answer_text>x = 4</wrong_answer_text>
            <mistake_status>wrong_attempt</mistake_status>
            <mistake_analysis>移项错误</mistake_analysis>
            <question_text>求最小值</question_text>
            <answer_text>0</answer_text>
            <analysis>开口向上</analysis>
        """.trimIndent()

        val result = parser.parse(response)

        assertThat(result.subject).isEqualTo("数学")
        assertThat(result.knowledgePoints).containsExactly("二次函数", "函数最值")
        assertThat(result.requiresImage).isTrue()
        assertThat(result.wrongAnswerText).isEqualTo("x = 4")
        assertThat(result.mistakeStatus).isEqualTo(MistakeStatus.WRONG_ATTEMPT)
        assertThat(result.mistakeAnalysis).isEqualTo("移项错误")
        assertThat(result.questionText).isEqualTo("求最小值")
        assertThat(result.answerText).isEqualTo("0")
        assertThat(result.analysis).isEqualTo("开口向上")
    }

    @Test
    fun `missing required tag throws invalid response`() {
        val response = """
            <question_text>题目</question_text>
            <answer_text>答案</answer_text>
        """.trimIndent()

        val error = assertThrows(AiError.InvalidResponse::class.java) {
            parser.parse(response)
        }
        assertThat(error.diagnosticCode).contains("missing_tag")
    }

    @Test
    fun `unknown mistake status is rejected`() {
        val response = validResponse(mistakeStatus = "maybe_wrong")

        val error = assertThrows(AiError.InvalidResponse::class.java) {
            parser.parse(response)
        }
        assertThat(error.diagnosticCode).isEqualTo("analysis.invalid_mistake_status")
    }

    @Test
    fun `more than five knowledge points is rejected`() {
        val response = validResponse(
            knowledgePoints = "一,二,三,四,五,六",
        )

        val error = assertThrows(AiError.InvalidResponse::class.java) {
            parser.parse(response)
        }
        assertThat(error.diagnosticCode).isEqualTo("analysis.too_many_knowledge_points")
    }

    @Test
    fun `wrong answer text forces wrong attempt status`() {
        val response = validResponse(
            wrongAnswerText = "错误",
            mistakeStatus = "unknown",
        )

        val result = parser.parse(response)
        assertThat(result.mistakeStatus).isEqualTo(MistakeStatus.WRONG_ATTEMPT)
    }

    @Test
    fun `invalid subject falls back to other`() {
        val response = validResponse(subject = "天文")
        val result = parser.parse(response)
        assertThat(result.subject).isEqualTo("其他")
    }

    private fun validResponse(
        subject: String = "数学",
        knowledgePoints: String = "函数",
        wrongAnswerText: String = "",
        mistakeStatus: String = "unknown",
    ): String = """
        <subject>$subject</subject>
        <knowledge_points>$knowledgePoints</knowledge_points>
        <requires_image>false</requires_image>
        <wrong_answer_text>$wrongAnswerText</wrong_answer_text>
        <mistake_status>$mistakeStatus</mistake_status>
        <mistake_analysis></mistake_analysis>
        <question_text>题目</question_text>
        <answer_text>答案</answer_text>
        <analysis>解析</analysis>
    """.trimIndent()
}
