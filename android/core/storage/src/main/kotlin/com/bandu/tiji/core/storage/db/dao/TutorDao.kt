package com.bandu.tiji.core.storage.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.bandu.tiji.core.storage.db.entity.ExerciseEntity
import com.bandu.tiji.core.storage.db.entity.TutorMessageEntity
import com.bandu.tiji.core.storage.db.entity.TutorSessionEntity

@Dao
interface TutorDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSession(session: TutorSessionEntity)

    @Update
    suspend fun updateSession(session: TutorSessionEntity): Int

    @Delete
    suspend fun deleteSession(session: TutorSessionEntity): Int

    @Query("SELECT * FROM tutor_sessions WHERE id = :id")
    suspend fun getSession(id: String): TutorSessionEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertMessage(message: TutorMessageEntity)

    @Query(
        """
        SELECT * FROM tutor_messages
        WHERE session_id = :sessionId
        ORDER BY sequence
        """,
    )
    suspend fun getMessages(sessionId: String): List<TutorMessageEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertExercise(exercise: ExerciseEntity)

    @Update
    suspend fun updateExercise(exercise: ExerciseEntity): Int

    @Query(
        """
        SELECT * FROM exercises
        WHERE session_id = :sessionId
        ORDER BY created_at, id
        """,
    )
    suspend fun getExercises(sessionId: String): List<ExerciseEntity>

    @Query("SELECT COUNT(*) FROM tutor_messages")
    suspend fun messageCount(): Int

    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun exerciseCount(): Int
}
