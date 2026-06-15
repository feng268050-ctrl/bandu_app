package com.bandu.tiji.core.testing.fake

import com.bandu.tiji.core.model.id.ErrorItemId
import com.bandu.tiji.core.model.id.TutorMessageId
import com.bandu.tiji.core.model.id.TutorSessionId
import com.bandu.tiji.core.model.tutor.TutorMessage
import com.bandu.tiji.core.model.tutor.TutorMessageRole
import com.bandu.tiji.core.model.tutor.TutorMessageStatus
import com.bandu.tiji.core.model.tutor.TutorSession
import com.bandu.tiji.core.model.tutor.TutorSessionSummary
import com.bandu.tiji.core.testing.fixture.DEFAULT_FIXTURE_EPOCH_MILLIS
import com.bandu.tiji.domain.repository.TutorRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class FakeTutorRepository(
    initialSessions: List<TutorSession> = emptyList(),
) : TutorRepository {
    private val sessions = MutableStateFlow(initialSessions.associateBy(TutorSession::id))
    private var nextSessionId = initialSessions.size + 1
    private var nextMessageId = initialSessions.sumOf { it.messages.size } + 1

    val failures = FailureInjector()
    val requestedErrorItemIds = mutableListOf<ErrorItemId?>()
    val userMessages = mutableListOf<Pair<TutorSessionId, String>>()
    val assistantMessages = mutableListOf<Pair<TutorSessionId, String>>()
    val deletedSessionIds = mutableListOf<TutorSessionId>()

    override fun observeSessions(): Flow<List<TutorSessionSummary>> =
        sessions.map { values ->
            values.values
                .sortedByDescending(TutorSession::updatedAtEpochMillis)
                .map { it.toSummary() }
        }

    override fun observeSession(id: TutorSessionId): Flow<TutorSession?> =
        sessions
            .map { it[id] }
            .distinctUntilChanged()

    override suspend fun getOrCreate(errorItemId: ErrorItemId?): TutorSessionId {
        failures.throwIfQueued()
        requestedErrorItemIds += errorItemId
        sessions.value.values.firstOrNull { it.errorItemId == errorItemId }?.let { return it.id }

        val id = TutorSessionId("session-${nextSessionId++}")
        sessions.value +=
            id to
                TutorSession(
                    id = id,
                    title = "Tutor session",
                    errorItemId = errorItemId,
                    messages = emptyList(),
                    exercises = emptyList(),
                    createdAtEpochMillis = DEFAULT_FIXTURE_EPOCH_MILLIS,
                    updatedAtEpochMillis = DEFAULT_FIXTURE_EPOCH_MILLIS,
                )
        return id
    }

    override suspend fun appendUserMessage(
        sessionId: TutorSessionId,
        text: String,
    ): TutorMessageId {
        failures.throwIfQueued()
        userMessages += sessionId to text
        return appendMessage(sessionId, text, TutorMessageRole.USER)
    }

    override suspend fun appendAssistantMessage(
        sessionId: TutorSessionId,
        text: String,
    ): TutorMessageId {
        failures.throwIfQueued()
        assistantMessages += sessionId to text
        return appendMessage(sessionId, text, TutorMessageRole.ASSISTANT)
    }

    override suspend fun deleteSession(id: TutorSessionId) {
        failures.throwIfQueued()
        deletedSessionIds += id
        sessions.value -= id
    }

    fun emit(session: TutorSession) {
        sessions.value += session.id to session
    }

    fun emit(sessions: List<TutorSession>) {
        this.sessions.value = sessions.associateBy(TutorSession::id)
    }

    private fun appendMessage(
        sessionId: TutorSessionId,
        text: String,
        role: TutorMessageRole,
    ): TutorMessageId {
        val session = checkNotNull(sessions.value[sessionId]) { "Unknown session: ${sessionId.value}" }
        val id = TutorMessageId("message-${nextMessageId++}")
        val message =
            TutorMessage(
                id = id,
                role = role,
                content = text,
                status = TutorMessageStatus.COMPLETE,
                sequence = session.messages.size,
                createdAtEpochMillis = DEFAULT_FIXTURE_EPOCH_MILLIS,
            )
        sessions.value +=
            sessionId to
                session.copy(
                    messages = session.messages + message,
                    updatedAtEpochMillis = DEFAULT_FIXTURE_EPOCH_MILLIS,
                )
        return id
    }

    private fun TutorSession.toSummary(): TutorSessionSummary =
        TutorSessionSummary(
            id = id,
            title = title,
            errorItemId = errorItemId,
            updatedAtEpochMillis = updatedAtEpochMillis,
        )
}
