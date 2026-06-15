package com.bandu.tiji.ai.api.prompt

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PromptTypeTest {
    @Test
    fun `all four prompt types expose required placeholders`() {
        assertThat(PromptType.ALL).hasSize(4)
        assertThat(PromptType.ANALYZE_IMAGE.requiredPlaceholders).containsExactly(
            "{{language_instruction}}",
            "{{knowledge_points_list}}",
            "{{grade_instruction}}",
            "{{provider_hints}}",
        )
        assertThat(PromptType.TUTOR.requiredPlaceholders).containsExactly(
            "{{question_context}}",
            "{{conversation_context}}",
            "{{user_message}}",
            "{{grade_instruction}}",
        )
        assertThat(PromptType.GENERATE_EXERCISE.requiredPlaceholders).containsExactly(
            "{{original_question}}",
            "{{knowledge_points}}",
            "{{difficulty_level}}",
            "{{grade_instruction}}",
        )
        assertThat(PromptType.GRADE_EXERCISE.requiredPlaceholders).containsExactly(
            "{{exercise_question}}",
            "{{expected_answer}}",
            "{{user_answer}}",
            "{{rubric_context}}",
        )
    }
}
