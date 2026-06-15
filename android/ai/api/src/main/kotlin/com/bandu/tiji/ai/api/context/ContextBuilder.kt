package com.bandu.tiji.ai.api.context

import com.bandu.tiji.ai.api.model.TutorContextMessage
import com.bandu.tiji.core.model.tutor.TutorMessageRole

data class BoundErrorItemContext(
    val questionText: String,
    val wrongAnswerText: String,
    val mistakeAnalysis: String,
    val answerText: String,
    val analysis: String,
    val tagNames: List<String>,
) {
    init {
        require(questionText.isNotBlank()) { "questionText must not be blank" }
    }
}

data class ContextBuildInput(
    val systemPrompt: String,
    val userMessage: String,
    val boundErrorItem: BoundErrorItemContext?,
    val recentMessages: List<TutorContextMessage>,
    val totalBudget: Int = ContextBuilder.DEFAULT_TOTAL_BUDGET,
    val errorItemBudget: Int = ContextBuilder.DEFAULT_ERROR_ITEM_BUDGET,
) {
    init {
        require(totalBudget > 0) { "totalBudget must be positive" }
        require(errorItemBudget > 0) { "errorItemBudget must be positive" }
        require(userMessage.isNotBlank()) { "userMessage must not be blank" }
    }
}

data class BuiltTutorContext(
    val systemPrompt: String,
    val userMessage: String,
    val questionContext: String,
    val conversationContext: String,
)

class ContextBuilder {
    fun build(input: ContextBuildInput): BuiltTutorContext {
        val reserved = codePointCount(input.systemPrompt) + codePointCount(input.userMessage)
        var remainingBudget = (input.totalBudget - reserved).coerceAtLeast(0)

        val errorBudget = minOf(input.errorItemBudget, remainingBudget)
        val questionContext = input.boundErrorItem?.let { item ->
            buildErrorItemContext(item, errorBudget)
        }.orEmpty()
        remainingBudget = (remainingBudget - codePointCount(questionContext)).coerceAtLeast(0)

        val conversationContext = buildConversationContext(input.recentMessages, remainingBudget)

        return BuiltTutorContext(
            systemPrompt = input.systemPrompt,
            userMessage = input.userMessage,
            questionContext = questionContext,
            conversationContext = conversationContext,
        )
    }

    private fun buildErrorItemContext(
        item: BoundErrorItemContext,
        budget: Int,
    ): String {
        val tagsSection = if (item.tagNames.isEmpty()) {
            ""
        } else {
            "标签：${item.tagNames.joinToString("、")}"
        }

        val fixedSections = listOf(
            "题目：${item.questionText}",
            "学生错误答案：${item.wrongAnswerText}",
            "正确答案：${item.answerText}",
            tagsSection,
        ).filter { it.isNotBlank() }

        val truncatableSections = listOf(
            "错误分析：${item.mistakeAnalysis}",
            "解析：${item.analysis}",
        )

        val fixedText = fixedSections.joinToString("\n")
        val fixedLength = codePointCount(fixedText)
        val truncatableBudget = (budget - fixedLength).coerceAtLeast(0)

        val truncatableText = joinWithTruncation(truncatableSections, truncatableBudget)
        return listOf(fixedText, truncatableText)
            .filter { it.isNotBlank() }
            .joinToString("\n")
    }

    private fun joinWithTruncation(sections: List<String>, budget: Int): String {
        if (budget <= 0 || sections.isEmpty()) return ""

        val fullText = sections.joinToString("\n")
        val fullLength = codePointCount(fullText)
        if (fullLength <= budget) return fullText

        val builder = StringBuilder()
        var used = 0
        sections.forEachIndexed { index, section ->
            if (index > 0) {
                val separatorLength = 1
                if (used + separatorLength > budget) return@forEachIndexed
                builder.append('\n')
                used += separatorLength
            }

            val available = budget - used
            if (available <= 0) return@forEachIndexed

            val sectionLength = codePointCount(section)
            if (sectionLength <= available) {
                builder.append(section)
                used += sectionLength
            } else {
                val truncated = truncateToCodePoints(section, available - codePointCount(TRUNCATION_MARKER))
                builder.append(truncated)
                builder.append(TRUNCATION_MARKER)
                used = budget
            }
        }
        return builder.toString()
    }

    private fun buildConversationContext(
        messages: List<TutorContextMessage>,
        budget: Int,
    ): String {
        if (budget <= 0 || messages.isEmpty()) return ""

        val selected = ArrayDeque<String>()
        var used = 0

        for (message in messages.asReversed()) {
            val formatted = formatMessage(message)
            val length = codePointCount(formatted)
            if (length > budget - used) {
                break
            }
            selected.addFirst(formatted)
            used += length
        }

        return selected.joinToString("\n\n")
    }

    private fun formatMessage(message: TutorContextMessage): String {
        val roleLabel = when (message.role) {
            TutorMessageRole.USER -> "学生"
            TutorMessageRole.ASSISTANT -> "辅导老师"
            TutorMessageRole.SYSTEM_LOCAL -> "系统"
        }
        return "$roleLabel：${message.content}"
    }

    private fun codePointCount(text: String): Int = text.codePointCount(0, text.length)

    private fun truncateToCodePoints(text: String, maxCodePoints: Int): String {
        if (maxCodePoints <= 0) return ""
        val endIndex = text.offsetByCodePoints(0, minOf(maxCodePoints, text.codePointCount(0, text.length)))
        return text.substring(0, endIndex)
    }

    companion object {
        const val DEFAULT_TOTAL_BUDGET = 24_000
        const val DEFAULT_ERROR_ITEM_BUDGET = 12_000
        const val TRUNCATION_MARKER = "\n[内容已截断]"
    }
}
