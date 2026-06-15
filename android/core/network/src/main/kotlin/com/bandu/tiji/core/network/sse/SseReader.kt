package com.bandu.tiji.core.network.sse

import okio.BufferedSource

sealed interface SseEvent {
    data class Data(
        val data: String,
        val event: String? = null,
        val id: String? = null,
    ) : SseEvent

    data object Done : SseEvent
}

class SseReader {
    fun read(source: BufferedSource): List<SseEvent> {
        val events = mutableListOf<SseEvent>()
        readLines(source) { event ->
            events += event
            event != SseEvent.Done
        }
        return events
    }

    suspend fun readEach(
        source: BufferedSource,
        onEvent: suspend (SseEvent) -> Unit,
    ) {
        readLinesSuspending(source) { event ->
            onEvent(event)
            event != SseEvent.Done
        }
    }

    private fun readLines(
        source: BufferedSource,
        onEvent: (SseEvent) -> Boolean,
    ) {
        val parser = Parser()
        while (!source.exhausted()) {
            val line = source.readUtf8Line() ?: break
            val event = parser.accept(line) ?: continue
            if (!onEvent(event)) return
        }
        parser.finish()?.let(onEvent)
    }

    private suspend fun readLinesSuspending(
        source: BufferedSource,
        onEvent: suspend (SseEvent) -> Boolean,
    ) {
        val parser = Parser()
        while (!source.exhausted()) {
            val line = source.readUtf8Line() ?: break
            val event = parser.accept(line) ?: continue
            if (!onEvent(event)) return
        }
        parser.finish()?.let { onEvent(it) }
    }

    private class Parser {
        val dataLines = mutableListOf<String>()
        var eventType: String? = null
        var eventId: String? = null

        fun accept(line: String): SseEvent? {
            if (line.isEmpty()) return dispatch()
            if (line.startsWith(':')) return null

            val separator = line.indexOf(':')
            val field = if (separator >= 0) line.substring(0, separator) else line
            val rawValue = if (separator >= 0) line.substring(separator + 1) else ""
            val value = rawValue.removePrefix(" ")
            when (field) {
                "data" -> dataLines += value
                "event" -> eventType = value
                "id" -> if (!value.contains('\u0000')) eventId = value
            }
            return null
        }

        fun finish(): SseEvent? = dispatch()

        private fun dispatch(): SseEvent? {
            if (dataLines.isEmpty()) {
                eventType = null
                return null
            }
            val data = dataLines.joinToString("\n")
            dataLines.clear()
            if (data == "[DONE]") {
                return SseEvent.Done
            }
            return SseEvent.Data(
                data = data,
                event = eventType,
                id = eventId,
            ).also {
                eventType = null
            }
        }
    }
}
