package com.bandu.tiji.ai.api.model

import com.bandu.tiji.core.model.id.TutorSessionId
import com.bandu.tiji.core.model.tutor.TutorMessageRole

data class TutorContextMessage(
    val role: TutorMessageRole,
    val content: String,
) {
    init {
        require(content.isNotBlank()) { "content must not be blank" }
    }
}

data class TutorRequest(
    val sessionId: TutorSessionId,
    val userMessage: String,
    val questionContext: String,
    val conversationContext: String,
    val gradeInstruction: String = "",
) {
    init {
        require(userMessage.isNotBlank()) { "userMessage must not be blank" }
    }
}

sealed interface AiStreamEvent {
    data class Delta(val text: String) : AiStreamEvent

    data class Usage(val inputTokens: Long?, val outputTokens: Long?) : AiStreamEvent

    data object Completed : AiStreamEvent
}
