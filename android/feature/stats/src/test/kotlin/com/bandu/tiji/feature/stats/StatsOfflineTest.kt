package com.bandu.tiji.feature.stats

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToIndex
import com.bandu.tiji.core.designsystem.theme.BanduTijiTheme
import com.bandu.tiji.core.model.enums.ExerciseDifficulty
import com.bandu.tiji.core.model.stats.ExerciseStats
import com.bandu.tiji.core.model.stats.MonthlyCount
import com.bandu.tiji.core.model.stats.WrongItemStats
import com.bandu.tiji.core.testing.fake.FakeStatsRepository
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h800dp")
class StatsOfflineTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `fake local repository renders complete stats page without network`() {
        val viewModel = StatsViewModel(
            FakeStatsRepository(
                initialWrongItemStats = WrongItemStats(
                    totalCount = 6,
                    masteredCount = 4,
                    subjectCounts = mapOf("数学" to 6),
                    monthlyNewCounts = listOf(MonthlyCount(2026, 6, 6)),
                ),
                initialExerciseStats = ExerciseStats(
                    totalCount = 5,
                    gradedCount = 4,
                    correctCount = 3,
                    subjectCounts = mapOf("数学" to 5),
                    difficultyCounts = mapOf(ExerciseDifficulty.MEDIUM to 5),
                    monthlyPracticeCounts = listOf(MonthlyCount(2026, 6, 5)),
                    activeDaysLastSixMonths = 3,
                ),
            ),
        )
        composeRule.setContent {
            BanduTijiTheme {
                StatsRoute(viewModel)
            }
        }

        listOf(
            0 to "错题统计",
            4 to "错题学科分布",
            6 to "最近 6 个月新增趋势",
            8 to "AI 练习统计",
            12 to "练习学科分布",
            14 to "练习难度分布",
            16 to "最近 6 个月练习趋势",
        ).forEach { (index, title) ->
            composeRule.onNodeWithTag("stats-list").performScrollToIndex(index)
            composeRule.onNodeWithText(title).assertIsDisplayed()
        }
    }
}
