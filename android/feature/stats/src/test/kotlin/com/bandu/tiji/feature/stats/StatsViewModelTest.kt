package com.bandu.tiji.feature.stats

import com.bandu.tiji.core.model.enums.ExerciseDifficulty
import com.bandu.tiji.core.model.stats.ExerciseStats
import com.bandu.tiji.core.model.stats.WrongItemStats
import com.bandu.tiji.core.testing.coroutines.MainDispatcherRule
import com.bandu.tiji.core.testing.fake.FakeStatsRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `repository flow changes refresh only their local section`() = runTest {
        val initialWrongItems = wrongItemStats(total = 3, mastered = 1)
        val initialExercises = exerciseStats(total = 4, graded = 3, correct = 2)
        val repository = FakeStatsRepository(initialWrongItems, initialExercises)
        val viewModel = StatsViewModel(repository)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value).isEqualTo(
            StatsUiState(
                wrongItemStats = initialWrongItems,
                exerciseStats = initialExercises,
                isLoading = false,
            ),
        )

        val updatedWrongItems = wrongItemStats(total = 8, mastered = 5)
        repository.emitWrongItemStats(updatedWrongItems)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.wrongItemStats).isEqualTo(updatedWrongItems)
        assertThat(viewModel.uiState.value.exerciseStats).isEqualTo(initialExercises)

        val updatedExercises = exerciseStats(total = 9, graded = 8, correct = 6)
        repository.emitExerciseStats(updatedExercises)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.wrongItemStats).isEqualTo(updatedWrongItems)
        assertThat(viewModel.uiState.value.exerciseStats).isEqualTo(updatedExercises)
    }

    private fun wrongItemStats(total: Int, mastered: Int) =
        WrongItemStats(
            totalCount = total,
            masteredCount = mastered,
            subjectCounts = emptyMap(),
            monthlyNewCounts = emptyList(),
        )

    private fun exerciseStats(total: Int, graded: Int, correct: Int) =
        ExerciseStats(
            totalCount = total,
            gradedCount = graded,
            correctCount = correct,
            subjectCounts = emptyMap(),
            difficultyCounts = emptyMap<ExerciseDifficulty, Int>(),
            monthlyPracticeCounts = emptyList(),
            activeDaysLastSixMonths = 0,
        )
}
