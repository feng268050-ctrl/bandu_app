package com.bandu.tiji.ai.api.prompt

sealed interface PromptValidationResult {
    data class Valid(val template: String) : PromptValidationResult

    data class Invalid(val code: String, val details: List<String> = emptyList()) : PromptValidationResult
}

class PromptRenderer {
    fun validate(type: PromptType, template: String): PromptValidationResult {
        val placeholders = PLACEHOLDER_REGEX.findAll(template).map { it.value }.toList()

        val missing = type.requiredPlaceholders.filter { required ->
            placeholders.none { it == required }
        }
        if (missing.isNotEmpty()) {
            return PromptValidationResult.Invalid(
                code = "prompt.missing_placeholders",
                details = missing,
            )
        }

        val unknown = placeholders.filter { it !in type.allowedPlaceholders }
        if (unknown.isNotEmpty()) {
            return PromptValidationResult.Invalid(
                code = "prompt.unknown_placeholders",
                details = unknown,
            )
        }

        if (template.toByteArray(Charsets.UTF_8).size > MAX_TEMPLATE_BYTES) {
            return PromptValidationResult.Invalid(code = "prompt.too_long")
        }

        val sampleValues = type.requiredPlaceholders.associate { placeholder ->
            placeholderToKey(placeholder) to SAMPLE_VALUE
        }
        val rendered = render(template, sampleValues)
        if (UNRENDERED_PLACEHOLDER_REGEX.containsMatchIn(rendered)) {
            return PromptValidationResult.Invalid(code = "prompt.unrendered_placeholders")
        }

        return PromptValidationResult.Valid(template)
    }

    fun render(template: String, values: Map<String, String>): String =
        PLACEHOLDER_REGEX.replace(template) { match ->
            val key = match.groupValues[1]
            values[key].orEmpty()
        }

    private fun placeholderToKey(placeholder: String): String =
        placeholder.removePrefix("{{").removeSuffix("}}")

    companion object {
        const val MAX_TEMPLATE_BYTES = 64 * 1024
        private const val SAMPLE_VALUE = "sample"

        private val PLACEHOLDER_REGEX = Regex("""\{\{([a-z_]+)}}""")
        private val UNRENDERED_PLACEHOLDER_REGEX = Regex("""\{\{[^}]+\}}""")
    }
}
