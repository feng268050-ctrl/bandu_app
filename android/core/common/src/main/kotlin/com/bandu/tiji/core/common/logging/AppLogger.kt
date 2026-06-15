package com.bandu.tiji.core.common.logging

interface AppLogger {
    fun debug(event: String, fields: Map<String, Any?> = emptyMap())

    fun info(event: String, fields: Map<String, Any?> = emptyMap())

    fun warn(event: String, fields: Map<String, Any?> = emptyMap())

    fun error(event: String, throwable: Throwable? = null, fields: Map<String, Any?> = emptyMap())
}

class DebugAppLogger(
    private val delegate: (level: String, event: String, fields: Map<String, Any?>, throwable: Throwable?) -> Unit,
) : AppLogger {
    override fun debug(event: String, fields: Map<String, Any?>) {
        delegate("debug", event, LogSanitizer.sanitizeFields(fields), null)
    }

    override fun info(event: String, fields: Map<String, Any?>) {
        delegate("info", event, LogSanitizer.sanitizeFields(fields), null)
    }

    override fun warn(event: String, fields: Map<String, Any?>) {
        delegate("warn", event, LogSanitizer.sanitizeFields(fields), null)
    }

    override fun error(event: String, throwable: Throwable?, fields: Map<String, Any?>) {
        delegate("error", event, LogSanitizer.sanitizeFields(fields), throwable)
    }
}

class ReleaseAppLogger : AppLogger {
    override fun debug(event: String, fields: Map<String, Any?>) = Unit

    override fun info(event: String, fields: Map<String, Any?>) = Unit

    override fun warn(event: String, fields: Map<String, Any?>) = Unit

    override fun error(event: String, throwable: Throwable?, fields: Map<String, Any?>) = Unit
}
