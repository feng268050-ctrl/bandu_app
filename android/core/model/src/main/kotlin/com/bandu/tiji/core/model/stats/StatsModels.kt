package com.bandu.tiji.core.model.stats

import com.bandu.tiji.core.model.enums.ExerciseDifficulty
import com.bandu.tiji.core.model.enums.GradeResult

data class WrongItemStats(
    val totalCount: Int,
    val masteredCount: Int,
    val subjectCounts: Map<String, Int>,
    val monthlyNewCounts: List<MonthlyCount>,
) {
    val masteryRate: Double
        get() = if (totalCount == 0) 0.0 else masteredCount.toDouble() / totalCount.toDouble()
}

data class ExerciseStats(
    val totalCount: Int,
    val gradedCount: Int,
    val correctCount: Int,
    val subjectCounts: Map<String, Int>,
    val difficultyCounts: Map<ExerciseDifficulty, Int>,
    val monthlyPracticeCounts: List<MonthlyCount>,
    val activeDaysLastSixMonths: Int,
) {
    val accuracyRate: Double
        get() = if (gradedCount == 0) 0.0 else correctCount.toDouble() / gradedCount.toDouble()

    companion object {
        fun fromResults(results: List<GradeResult?>): ExerciseStats {
            val effective = results.mapNotNull { it }
            val graded = effective.filter { it != GradeResult.NEEDS_REVIEW }
            val correct = graded.count { it == GradeResult.CORRECT }
            return ExerciseStats(
                totalCount = effective.size,
                gradedCount = graded.size,
                correctCount = correct,
                subjectCounts = emptyMap(),
                difficultyCounts = emptyMap(),
                monthlyPracticeCounts = emptyList(),
                activeDaysLastSixMonths = 0,
            )
        }
    }
}

data class MonthlyCount(
    val year: Int,
    val month: Int,
    val count: Int,
)
