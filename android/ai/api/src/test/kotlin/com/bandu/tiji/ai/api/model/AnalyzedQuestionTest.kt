package com.bandu.tiji.ai.api.model

import com.bandu.tiji.core.model.enums.MistakeStatus
import org.junit.Assert.assertThrows
import org.junit.Test

class AnalyzedQuestionTest {
    @Test
    fun `valid analyzed question accepts nine fields`() {
        AnalyzedQuestion(
            subject = "数学",
            knowledgePoints = listOf("函数"),
            requiresImage = true,
            wrongAnswerText = "x=1",
            mistakeStatus = MistakeStatus.WRONG_ATTEMPT,
            mistakeAnalysis = "计算错误",
            questionText = "求值",
            answerText = "x=2",
            analysis = "移项求解",
        )
    }

    @Test
    fun `rejects blank required text fields`() {
        assertThrows(IllegalArgumentException::class.java) {
            AnalyzedQuestion(
                subject = "数学",
                knowledgePoints = emptyList(),
                requiresImage = false,
                wrongAnswerText = "",
                mistakeStatus = MistakeStatus.UNKNOWN,
                mistakeAnalysis = "",
                questionText = "",
                answerText = "a",
                analysis = "n",
            )
        }
    }

    @Test
    fun `rejects more than five knowledge points`() {
        assertThrows(IllegalArgumentException::class.java) {
            AnalyzedQuestion(
                subject = "数学",
                knowledgePoints = List(6) { "kp$it" },
                requiresImage = false,
                wrongAnswerText = "",
                mistakeStatus = MistakeStatus.UNKNOWN,
                mistakeAnalysis = "",
                questionText = "q",
                answerText = "a",
                analysis = "n",
            )
        }
    }
}
