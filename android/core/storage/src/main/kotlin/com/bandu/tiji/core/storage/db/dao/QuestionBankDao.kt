package com.bandu.tiji.core.storage.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.bandu.tiji.core.storage.db.entity.BankQuestionEntity
import com.bandu.tiji.core.storage.db.entity.ExamAttemptEntity
import com.bandu.tiji.core.storage.db.entity.ExamSessionEntity
import com.bandu.tiji.core.storage.db.entity.QuestionBankEntity
import com.bandu.tiji.core.storage.db.projection.ExamAttemptQuestionProjection
import com.bandu.tiji.core.storage.db.projection.QuestionBankSummaryProjection
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestionBankDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertBank(bank: QuestionBankEntity)

    @Update
    suspend fun updateBank(bank: QuestionBankEntity): Int

    @Query("SELECT * FROM question_banks WHERE id = :id")
    suspend fun getBank(id: String): QuestionBankEntity?

    @Query("SELECT * FROM question_banks WHERE id = :id")
    fun observeBank(id: String): Flow<QuestionBankEntity?>

    @Query(
        """
        SELECT
            b.id AS id,
            b.name AS name,
            b.source_file_name AS source_file_name,
            b.source_uri AS source_uri,
            b.subject AS subject,
            b.import_status AS import_status,
            COUNT(q.id) AS question_count,
            b.created_at AS created_at,
            b.updated_at AS updated_at
        FROM question_banks b
        LEFT JOIN bank_questions q ON q.bank_id = b.id
        GROUP BY b.id
        ORDER BY b.updated_at DESC, b.name COLLATE NOCASE
        """,
    )
    fun observeBankSummaries(): Flow<List<QuestionBankSummaryProjection>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertQuestion(question: BankQuestionEntity)

    @Query(
        """
        SELECT * FROM bank_questions
        WHERE bank_id = :bankId
        ORDER BY created_at, id
        """,
    )
    fun observeQuestions(bankId: String): Flow<List<BankQuestionEntity>>

    @Query(
        """
        SELECT * FROM bank_questions
        WHERE bank_id = :bankId
        ORDER BY created_at, id
        """,
    )
    suspend fun getQuestions(bankId: String): List<BankQuestionEntity>

    @Query("SELECT * FROM bank_questions WHERE id = :id")
    suspend fun getQuestion(id: String): BankQuestionEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSession(session: ExamSessionEntity)

    @Update
    suspend fun updateSession(session: ExamSessionEntity): Int

    @Query("SELECT * FROM exam_sessions WHERE id = :id")
    suspend fun getSession(id: String): ExamSessionEntity?

    @Query("SELECT * FROM exam_sessions WHERE id = :id")
    fun observeSession(id: String): Flow<ExamSessionEntity?>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAttempts(attempts: List<ExamAttemptEntity>)

    @Update
    suspend fun updateAttempt(attempt: ExamAttemptEntity): Int

    @Query("SELECT * FROM exam_attempts WHERE id = :id")
    suspend fun getAttempt(id: String): ExamAttemptEntity?

    @Query(
        """
        SELECT COUNT(*) FROM exam_attempts
        WHERE session_id = :sessionId AND answer_revealed = 0
        """,
    )
    suspend fun countUnrevealedAttempts(sessionId: String): Int

    @Query(
        """
        SELECT
            a.id AS attempt_id,
            a.session_id AS session_id,
            a.order_index AS order_index,
            a.user_answer AS user_answer,
            a.answer_revealed AS answer_revealed,
            a.grading_result AS grading_result,
            a.grading_source AS grading_source,
            a.grading_feedback AS grading_feedback,
            a.submitted_at AS submitted_at,
            q.id AS question_id,
            q.bank_id AS bank_id,
            q.stem AS stem,
            q.options_text AS options_text,
            q.answer AS answer,
            q.analysis AS analysis,
            q.question_type AS question_type,
            q.difficulty AS difficulty,
            q.tags_text AS tags_text,
            q.source_page AS source_page,
            q.source_text AS source_text,
            q.source_image_uri AS source_image_uri,
            q.review_status AS review_status,
            q.created_at AS question_created_at,
            q.updated_at AS question_updated_at
        FROM exam_attempts a
        INNER JOIN bank_questions q ON q.id = a.question_id
        WHERE a.session_id = :sessionId
        ORDER BY a.order_index
        """,
    )
    fun observeAttemptQuestions(sessionId: String): Flow<List<ExamAttemptQuestionProjection>>
}
