package com.bandu.tiji.ai.api.prompt

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PromptRendererTest {
    private val renderer = PromptRenderer()

    @Test
    fun `AC-AI-003 rejects missing required placeholders`() {
        val template = "只有 {{language_instruction}}"

        val result = renderer.validate(PromptType.ANALYZE_IMAGE, template)

        assertThat(result).isInstanceOf(PromptValidationResult.Invalid::class.java)
        val invalid = result as PromptValidationResult.Invalid
        assertThat(invalid.code).isEqualTo("prompt.missing_placeholders")
        assertThat(invalid.details).isNotEmpty()
    }

    @Test
    fun `rejects unknown placeholders`() {
        val template = """
            {{language_instruction}}
            {{knowledge_points_list}}
            {{grade_instruction}}
            {{provider_hints}}
            {{unknown_field}}
        """.trimIndent()

        val result = renderer.validate(PromptType.ANALYZE_IMAGE, template)

        assertThat(result).isInstanceOf(PromptValidationResult.Invalid::class.java)
        val invalid = result as PromptValidationResult.Invalid
        assertThat(invalid.code).isEqualTo("prompt.unknown_placeholders")
        assertThat(invalid.details).contains("{{unknown_field}}")
    }

    @Test
    fun `valid template renders without leftover placeholders`() {
        val template = """
            语言：{{language_instruction}}
            标签：{{knowledge_points_list}}
            年级：{{grade_instruction}}
            提示：{{provider_hints}}
        """.trimIndent()

        val validation = renderer.validate(PromptType.ANALYZE_IMAGE, template)
        assertThat(validation).isInstanceOf(PromptValidationResult.Valid::class.java)

        val rendered = renderer.render(
            template,
            mapOf(
                "language_instruction" to "简体中文",
                "knowledge_points_list" to "函数",
                "grade_instruction" to "初二",
                "provider_hints" to "",
            ),
        )
        assertThat(rendered).contains("简体中文")
        assertThat(rendered).doesNotContain("{{")
    }
}
