package com.bandu.tiji.data.repository

import androidx.room.withTransaction
import com.bandu.tiji.core.common.id.UuidGenerator
import com.bandu.tiji.core.common.time.Clock
import com.bandu.tiji.core.model.id.ErrorItemId
import com.bandu.tiji.core.model.id.TutorMessageId
import com.bandu.tiji.core.model.id.TutorSessionId
import com.bandu.tiji.core.model.tutor.TutorMessageRole
import com.bandu.tiji.core.model.tutor.TutorMessageStatus
import com.bandu.tiji.core.model.tutor.TutorSession
import com.bandu.tiji.core.model.tutor.TutorSessionSummary
import com.bandu.tiji.core.storage.db.LearningDatabase
import com.bandu.tiji.core.storage.db.entity.TutorMessageEntity
import com.bandu.tiji.core.storage.db.entity.TutorSessionEntity
import com.bandu.tiji.data.mapper.toDomain
import com.bandu.tiji.data.mapper.toDomainSummary
import com.bandu.tiji.domain.repository.TutorRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
class RoomTutorRepository @Inject constructor(
    private val database: LearningDatabase,
    private val uuidGenerator: UuidGenerator,
    private val clock: Clock,
) : TutorRepository {
    private val tutorDao = database.tutorDao()
    private val appendMutex = Mutex()

    override fun observeSessions(): Flow<List<TutorSessionSummary>> =
        database.invalidationTracker
            .createFlow("tutor_sessions", emitInitialState = true)
            .map { database.readTutorSessions().map(TutorSessionEntity::toDomainSummary) }

    override fun observeSession(id: TutorSessionId): Flow<TutorSession?> =
        database.invalidationTracker
            .createFlow("tutor_sessions", "tutor_messages", "exercises", emitInitialState = true)
            .map {
                tutorDao.getSession(id.value)?.toDomain(
                    messages = tutorDao.getMessages(id.value).map { message -> message.toDomain() },
                    exercises = tutorDao.getExercises(id.value).map { exercise -> exercise.toDomain() },
                )
            }

    override suspend fun getOrCreate(errorItemId: ErrorItemId?): TutorSessionId =
        database.withTransaction {
            database.findTutorSession(errorItemId?.value)?.let { existing ->
                return@withTransaction TutorSessionId(existing.id)
            }
            val now = clock.nowEpochMillis()
            val id = TutorSessionId(uuidGenerator.newUuid())
            tutorDao.insertSession(
                TutorSessionEntity(
                    id = id.value,
                    title = DEFAULT_SESSION_TITLE,
                    errorItemId = errorItemId?.value,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
            id
        }

    override suspend fun appendUserMessage(
        sessionId: TutorSessionId,
        text: String,
    ): TutorMessageId =
        appendMessage(sessionId, text, TutorMessageRole.USER)

    override suspend fun appendAssistantMessage(
        sessionId: TutorSessionId,
        text: String,
    ): TutorMessageId =
        appendMessage(sessionId, text, TutorMessageRole.ASSISTANT)

    override suspend fun deleteSession(id: TutorSessionId) {
        database.withTransaction {
            tutorDao.getSession(id.value)?.let { tutorDao.deleteSession(it) }
        }
    }

    private suspend fun appendMessage(
        sessionId: TutorSessionId,
        text: String,
        role: TutorMessageRole,
    ): TutorMessageId =
        appendMutex.withLock {
            database.withTransaction {
                val session = requireNotNull(tutorDao.getSession(sessionId.value)) {
                    "Tutor session does not exist"
                }
                val now = clock.nowEpochMillis()
                val id = TutorMessageId(uuidGenerator.newUuid())
                tutorDao.insertMessage(
                    TutorMessageEntity(
                        id = id.value,
                        sessionId = sessionId.value,
                        role = role.name,
                        content = text,
                        status = TutorMessageStatus.COMPLETE.name,
                        sequence = database.nextTutorMessageSequence(sessionId.value),
                        createdAt = now,
                    ),
                )
                tutorDao.updateSession(session.copy(updatedAt = now))
                id
            }
        }

    private fun LearningDatabase.readTutorSessions(): List<TutorSessionEntity> =
        openHelper.readableDatabase
            .query("SELECT * FROM tutor_sessions ORDER BY updated_at DESC, id")
            .use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        add(
                            TutorSessionEntity(
                                id = cursor.string("id"),
                                title = cursor.string("title"),
                                errorItemId = cursor.nullableString("error_item_id"),
                                createdAt = cursor.long("created_at"),
                                updatedAt = cursor.long("updated_at"),
                            ),
                        )
                    }
                }
            }

    private fun LearningDatabase.findTutorSession(errorItemId: String?): TutorSessionEntity? {
        val (selection, arguments) = if (errorItemId == null) {
            "error_item_id IS NULL" to emptyArray()
        } else {
            "error_item_id = ?" to arrayOf(errorItemId)
        }
        return openHelper.readableDatabase
            .query(
                "SELECT * FROM tutor_sessions WHERE $selection ORDER BY updated_at DESC LIMIT 1",
                arguments,
            )
            .use { cursor ->
                if (!cursor.moveToFirst()) return@use null
                TutorSessionEntity(
                    id = cursor.string("id"),
                    title = cursor.string("title"),
                    errorItemId = cursor.nullableString("error_item_id"),
                    createdAt = cursor.long("created_at"),
                    updatedAt = cursor.long("updated_at"),
                )
            }
    }

    private fun LearningDatabase.nextTutorMessageSequence(sessionId: String): Int =
        openHelper.readableDatabase
            .query(
                "SELECT COALESCE(MAX(sequence), -1) + 1 FROM tutor_messages WHERE session_id = ?",
                arrayOf(sessionId),
            )
            .use { cursor -> if (cursor.moveToFirst()) cursor.getInt(0) else 0 }

    companion object {
        private const val DEFAULT_SESSION_TITLE = "新辅导会话"
    }
}

private fun android.database.Cursor.string(column: String): String =
    getString(getColumnIndexOrThrow(column))

private fun android.database.Cursor.nullableString(column: String): String? {
    val index = getColumnIndexOrThrow(column)
    return if (isNull(index)) null else getString(index)
}

private fun android.database.Cursor.long(column: String): Long =
    getLong(getColumnIndexOrThrow(column))
