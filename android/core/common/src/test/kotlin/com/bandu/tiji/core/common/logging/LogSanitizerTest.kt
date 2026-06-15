package com.bandu.tiji.core.common.logging

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LogSanitizerTest {
    @Test
    fun sanitizeFields_redactsSensitiveKeysAndLongValues() {
        val sanitized = LogSanitizer.sanitizeFields(
            mapOf(
                "apiKey" to "secret-value",
                "authorization" to "Bearer abc",
                "question" to "x".repeat(300),
                "count" to 3,
            ),
        )

        assertThat(sanitized["apiKey"]).isEqualTo("[REDACTED]")
        assertThat(sanitized["authorization"]).isEqualTo("[REDACTED]")
        assertThat(sanitized["question"]).isEqualTo("[REDACTED]")
        assertThat(sanitized["count"]).isEqualTo(3)
    }
}

class AppLoggerTest {
    @Test
    fun debugLogger_sanitizesBeforeDelegate() {
        val events = mutableListOf<Map<String, Any?>>()
        val logger = DebugAppLogger { _, _, fields, _ -> events += fields }

        logger.info("ai_request", mapOf("api_key" to "abc123"))

        assertThat(events.single()["api_key"]).isEqualTo("[REDACTED]")
    }

    @Test
    fun releaseLogger_doesNotEmit() {
        val logger = ReleaseAppLogger()
        logger.error("crash", RuntimeException("x"), mapOf("token" to "secret"))
    }
}
