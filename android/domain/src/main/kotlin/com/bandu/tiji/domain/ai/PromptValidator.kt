package com.bandu.tiji.domain.ai

import com.bandu.tiji.core.common.result.AppError
import com.bandu.tiji.core.common.result.AppResult

class PromptValidator {
    fun validate(type: PromptType, template: String): AppResult<String> {
        val placeholders = PLACEHOLDER_REGEX.findAll(template).map { it.value }.toList()
        val required = REQUIRED_PLACEHOLDERS[type].orEmpty()
        val allowed = ALLOWED_PLACEHOLDERS[type].orEmpty()

        val missing = required.filter { requiredPlaceholder ->
            placeholders.none { it == requiredPlaceholder }
        }
        if (missing.isNotEmpty()) {
            return AppResult.Failure(
                AppError.Validation("prompt.missing_placeholders:${missing.joinToString(",")}"),
            )
        }

        val unknown = placeholders.filter { it !in allowed }
        if (unknown.isNotEmpty()) {
            return AppResult.Failure(
                AppError.Validation("prompt.unknown_placeholders:${unknown.joinToString(",")}"),
            )
        }

        if (template.toByteArray(Charsets.UTF_8).size > MAX_TEMPLATE_BYTES) {
            return AppResult.Failure(AppError.Validation("prompt.too_long"))
        }

        val rendered = renderSample(type, template)
        if (PLACEHOLDER_REGEX.containsMatchIn(rendered)) {
            return AppResult.Failure(AppError.Validation("prompt.unrendered_placeholders"))
        }

        return AppResult.Success(template)
    }

    private fun renderSample(type: PromptType, template: String): String {
        var rendered = template
        ALLOWED_PLACEHOLDERS[type].orEmpty().forEach { placeholder ->
            rendered = rendered.replace(placeholder, "sample")
        }
        return rendered
    }

    companion object {
        const val MAX_TEMPLATE_BYTES = 64 * 1024

        private val PLACEHOLDER_REGEX = Regex("""\{\{[a-z_]+\}\}""")

        private val REQUIRED_PLACEHOLDERS = mapOf(
            PromptType.ANALYZE_IMAGE to setOf(
                "{{language_instruction}}",
                "{{knowledge_points_list}}",
                "{{grade_instruction}}",
                "{{provider_hints}}",
            ),
            PromptType.TUTOR to setOf(
                "{{question_context}}",
                "{{conversation_context}}",
                "{{user_message}}",
                "{{grade_instruction}}",
            ),
            PromptType.GENERATE_EXERCISE to setOf(
                "{{original_question}}",
                "{{knowledge_points}}",
                "{{difficulty_level}}",
                "{{grade_instruction}}",
            ),
            PromptType.GRADE_EXERCISE to setOf(
                "{{exercise_question}}",
                "{{expected_answer}}",
                "{{user_answer}}",
                "{{rubric_context}}",
            ),
        )

        private val ALLOWED_PLACEHOLDERS = REQUIRED_PLACEHOLDERS
    }
}
