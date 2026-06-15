package com.bandu.tiji.domain.repository

import com.bandu.tiji.core.model.id.ErrorItemId
import com.bandu.tiji.core.model.id.TutorMessageId
import com.bandu.tiji.core.model.id.TutorSessionId
import com.bandu.tiji.core.model.tutor.TutorSession
import com.bandu.tiji.core.model.tutor.TutorSessionSummary
import kotlinx.coroutines.flow.Flow

interface TutorRepository {
    fun observeSessions(): Flow<List<TutorSessionSummary>>

    fun observeSession(id: TutorSessionId): Flow<TutorSession?>

    suspend fun getOrCreate(errorItemId: ErrorItemId?): TutorSessionId

    suspend fun appendUserMessage(sessionId: TutorSessionId, text: String): TutorMessageId

    suspend fun appendAssistantMessage(sessionId: TutorSessionId, text: String): TutorMessageId

    suspend fun deleteSession(id: TutorSessionId)
}
