package com.bandu.tiji.ai.api.prompt

enum class PromptType {
    ANALYZE_IMAGE,
    TUTOR,
    GENERATE_EXERCISE,
    GRADE_EXERCISE,
    ;

    val requiredPlaceholders: Set<String>
        get() = REQUIRED_PLACEHOLDERS.getValue(this)

    val allowedPlaceholders: Set<String>
        get() = ALLOWED_PLACEHOLDERS.getValue(this)

    companion object {
        private val REQUIRED_PLACEHOLDERS = mapOf(
            ANALYZE_IMAGE to setOf(
                "{{language_instruction}}",
                "{{knowledge_points_list}}",
                "{{grade_instruction}}",
                "{{provider_hints}}",
            ),
            TUTOR to setOf(
                "{{question_context}}",
                "{{conversation_context}}",
                "{{user_message}}",
                "{{grade_instruction}}",
            ),
            GENERATE_EXERCISE to setOf(
                "{{original_question}}",
                "{{knowledge_points}}",
                "{{difficulty_level}}",
                "{{grade_instruction}}",
            ),
            GRADE_EXERCISE to setOf(
                "{{exercise_question}}",
                "{{expected_answer}}",
                "{{user_answer}}",
                "{{rubric_context}}",
            ),
        )

        private val ALLOWED_PLACEHOLDERS = REQUIRED_PLACEHOLDERS

        val ALL: List<PromptType> = entries
    }
}
