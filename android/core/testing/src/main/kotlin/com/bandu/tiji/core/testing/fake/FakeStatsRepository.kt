package com.bandu.tiji.core.testing.fake

import com.bandu.tiji.core.model.stats.ExerciseStats
import com.bandu.tiji.core.model.stats.WrongItemStats
import com.bandu.tiji.domain.repository.StatsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeStatsRepository(
    initialWrongItemStats: WrongItemStats = emptyWrongItemStats(),
    initialExerciseStats: ExerciseStats = emptyExerciseStats(),
) : StatsRepository {
    private val wrongItemStats = MutableStateFlow(initialWrongItemStats)
    private val exerciseStats = MutableStateFlow(initialExerciseStats)

    override fun observeWrongItemStats(): Flow<WrongItemStats> = wrongItemStats.asStateFlow()

    override fun observeExerciseStats(): Flow<ExerciseStats> = exerciseStats.asStateFlow()

    fun emitWrongItemStats(stats: WrongItemStats) {
        wrongItemStats.value = stats
    }

    fun emitExerciseStats(stats: ExerciseStats) {
        exerciseStats.value = stats
    }

    companion object {
        fun emptyWrongItemStats(): WrongItemStats =
            WrongItemStats(
                totalCount = 0,
                masteredCount = 0,
                subjectCounts = emptyMap(),
                monthlyNewCounts = emptyList(),
            )

        fun emptyExerciseStats(): ExerciseStats =
            ExerciseStats(
                totalCount = 0,
                gradedCount = 0,
                correctCount = 0,
                subjectCounts = emptyMap(),
                difficultyCounts = emptyMap(),
                monthlyPracticeCounts = emptyList(),
                activeDaysLastSixMonths = 0,
            )
    }
}
