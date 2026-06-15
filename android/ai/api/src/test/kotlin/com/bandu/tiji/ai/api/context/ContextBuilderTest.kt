package com.bandu.tiji.ai.api.context

import com.bandu.tiji.ai.api.model.TutorContextMessage
import com.bandu.tiji.core.model.tutor.TutorMessageRole
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ContextBuilderTest {
    private val builder = ContextBuilder()

    @Test
    fun `question text is never truncated when error item exceeds budget`() {
        val longAnalysis = "解".repeat(8_000)
        val longMistakeAnalysis = "错".repeat(8_000)
        val input = ContextBuildInput(
            systemPrompt = "系统",
            userMessage = "继续讲解",
            boundErrorItem = BoundErrorItemContext(
                questionText = "必须保留的完整题目",
                wrongAnswerText = "x=1",
                mistakeAnalysis = longMistakeAnalysis,
                answerText = "x=2",
                analysis = longAnalysis,
                tagNames = listOf("函数"),
            ),
            recentMessages = emptyList(),
            totalBudget = 500,
            errorItemBudget = 400,
        )

        val built = builder.build(input)

        assertThat(built.questionContext).contains("必须保留的完整题目")
        assertThat(built.questionContext).contains(ContextBuilder.TRUNCATION_MARKER)
        assertThat(built.userMessage).isEqualTo("继续讲解")
    }

    @Test
    fun `recent messages are included whole and oldest overflow is dropped`() {
        val messages = listOf(
            TutorContextMessage(TutorMessageRole.USER, "最早消息" + "a".repeat(200)),
            TutorContextMessage(TutorMessageRole.ASSISTANT, "中间消息"),
            TutorContextMessage(TutorMessageRole.USER, "最近消息"),
        )
        val input = ContextBuildInput(
            systemPrompt = "系统提示",
            userMessage = "当前问题",
            boundErrorItem = null,
            recentMessages = messages,
            totalBudget = 120,
        )

        val built = builder.build(input)

        assertThat(built.conversationContext).contains("最近消息")
        assertThat(built.conversationContext).contains("中间消息")
        assertThat(built.conversationContext).doesNotContain("最早消息")
    }

    @Test
    fun `uses default twenty four thousand character budget`() {
        assertThat(ContextBuilder.DEFAULT_TOTAL_BUDGET).isEqualTo(24_000)
        assertThat(ContextBuilder.DEFAULT_ERROR_ITEM_BUDGET).isEqualTo(12_000)
    }
}
