package com.bandu.tiji.feature.stats

import com.bandu.tiji.core.model.stats.ExerciseStats
import com.bandu.tiji.core.model.stats.WrongItemStats

data class StatsUiState(
    val wrongItemStats: WrongItemStats = emptyWrongItemStats(),
    val exerciseStats: ExerciseStats = emptyExerciseStats(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)

sealed interface StatsAction {
    data object Retry : StatsAction
}

internal fun emptyWrongItemStats() =
    WrongItemStats(
        totalCount = 0,
        masteredCount = 0,
        subjectCounts = emptyMap(),
        monthlyNewCounts = emptyList(),
    )

internal fun emptyExerciseStats() =
    ExerciseStats(
        totalCount = 0,
        gradedCount = 0,
        correctCount = 0,
        subjectCounts = emptyMap(),
        difficultyCounts = emptyMap(),
        monthlyPracticeCounts = emptyList(),
        activeDaysLastSixMonths = 0,
    )
