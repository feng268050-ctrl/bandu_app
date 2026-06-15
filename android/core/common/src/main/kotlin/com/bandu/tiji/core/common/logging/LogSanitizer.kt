package com.bandu.tiji.core.common.logging

object LogSanitizer {
    private val sensitiveKeyPattern = Regex(
        "(api[_-]?key|authorization|token|secret|password|pairing|session[_-]?key|nonce|srp)",
        RegexOption.IGNORE_CASE,
    )

    private val base64Pattern = Regex("^[A-Za-z0-9+/]{32,}={0,2}$")

    fun sanitizeFields(fields: Map<String, Any?>): Map<String, Any?> =
        fields.mapValues { (key, value) -> sanitizeValue(key, value) }

    fun sanitizeValue(key: String, value: Any?): Any? {
        if (value == null) return null
        if (sensitiveKeyPattern.containsMatchIn(key)) return REDACTED
        return when (value) {
            is String -> sanitizeString(value)
            is Map<*, *> -> value.entries.associate { (k, v) ->
                val childKey = k?.toString().orEmpty()
                childKey to sanitizeValue(childKey, v)
            }
            is Iterable<*> -> value.map { item ->
                if (item is String) sanitizeString(item) else item
            }
            else -> value
        }
    }

    private fun sanitizeString(value: String): String {
        if (value.length > 256) return REDACTED
        if (base64Pattern.matches(value)) return REDACTED
        if (value.contains("Bearer ", ignoreCase = true)) return REDACTED
        return value
    }

    private const val REDACTED = "[REDACTED]"
}
