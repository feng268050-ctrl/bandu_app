package com.bandu.tiji.core.storage.db.dao

import androidx.room.Dao
import androidx.room.Query
import com.bandu.tiji.core.storage.db.projection.DimensionCountProjection
import com.bandu.tiji.core.storage.db.projection.ExerciseTotalsProjection
import com.bandu.tiji.core.storage.db.projection.WrongItemTotalsProjection
import kotlinx.coroutines.flow.Flow

@Dao
interface StatsDao {
    @Query(
        """
        SELECT
            COUNT(*) AS total_count,
            COALESCE(SUM(CASE WHEN mastery_level = 2 THEN 1 ELSE 0 END), 0)
                AS mastered_count
        FROM error_items
        """,
    )
    fun observeWrongItemTotals(): Flow<WrongItemTotalsProjection>

    @Query(
        """
        SELECT subject AS dimension, COUNT(*) AS count
        FROM error_items
        GROUP BY subject
        ORDER BY count DESC, subject
        """,
    )
    fun observeWrongItemSubjectCounts(): Flow<List<DimensionCountProjection>>

    @Query(
        """
        SELECT COUNT(*) FROM error_items
        WHERE created_at >= :startInclusive AND created_at < :endExclusive
        """,
    )
    suspend fun countWrongItemsCreatedBetween(
        startInclusive: Long,
        endExclusive: Long,
    ): Int

    @Query(
        """
        SELECT
            COUNT(*) AS total_count,
            COALESCE(
                SUM(
                    CASE
                        WHEN COALESCE(final_result, ai_result)
                            IN ('CORRECT', 'INCORRECT')
                        THEN 1
                        ELSE 0
                    END
                ),
                0
            ) AS graded_count,
            COALESCE(
                SUM(
                    CASE
                        WHEN COALESCE(final_result, ai_result) = 'CORRECT'
                        THEN 1
                        ELSE 0
                    END
                ),
                0
            ) AS correct_count
        FROM exercises
        """,
    )
    fun observeExerciseTotals(): Flow<ExerciseTotalsProjection>

    @Query(
        """
        SELECT subject AS dimension, COUNT(*) AS count
        FROM exercises
        GROUP BY subject
        ORDER BY count DESC, subject
        """,
    )
    fun observeExerciseSubjectCounts(): Flow<List<DimensionCountProjection>>

    @Query(
        """
        SELECT difficulty AS dimension, COUNT(*) AS count
        FROM exercises
        GROUP BY difficulty
        ORDER BY count DESC, difficulty
        """,
    )
    fun observeExerciseDifficultyCounts(): Flow<List<DimensionCountProjection>>

    @Query(
        """
        SELECT COUNT(*) FROM exercises
        WHERE created_at >= :startInclusive AND created_at < :endExclusive
        """,
    )
    suspend fun countExercisesCreatedBetween(
        startInclusive: Long,
        endExclusive: Long,
    ): Int

    @Query(
        """
        SELECT COUNT(
            DISTINCT strftime(
                '%Y-%m-%d',
                created_at / 1000,
                'unixepoch',
                :utcOffsetModifier
            )
        )
        FROM exercises
        WHERE created_at >= :startInclusive AND created_at < :endExclusive
        """,
    )
    suspend fun countActiveExerciseDays(
        startInclusive: Long,
        endExclusive: Long,
        utcOffsetModifier: String,
    ): Int
}
