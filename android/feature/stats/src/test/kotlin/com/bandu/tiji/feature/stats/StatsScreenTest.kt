package com.bandu.tiji.feature.stats

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import com.bandu.tiji.core.designsystem.theme.BanduTijiTheme
import com.bandu.tiji.core.model.stats.MonthlyCount
import com.bandu.tiji.core.model.stats.WrongItemStats
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h800dp")
class StatsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `empty wrong item stats show zero totals and zero mastery rate`() {
        setStatsContent(StatsUiState(isLoading = false))

        composeRule.onNodeWithText("错题总数").assertIsDisplayed()
        composeRule.onNodeWithText("已掌握").assertIsDisplayed()
        composeRule.onNodeWithText("掌握率").assertIsDisplayed()
        composeRule.onNodeWithText("0%", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun `wrong item stats show total mastered and calculated mastery rate`() {
        setStatsContent(
            StatsUiState(
                wrongItemStats = WrongItemStats(
                    totalCount = 8,
                    masteredCount = 5,
                    subjectCounts = emptyMap(),
                    monthlyNewCounts = emptyList(),
                ),
                isLoading = false,
            ),
        )

        composeRule.onNodeWithText("8").assertIsDisplayed()
        composeRule.onNodeWithText("5").assertIsDisplayed()
        composeRule.onNodeWithText("62.5%").assertIsDisplayed()
    }

    @Test
    fun `wrong item charts show subject distribution and zero filled six month trend`() {
        setStatsContent(
            StatsUiState(
                wrongItemStats = WrongItemStats(
                    totalCount = 10,
                    masteredCount = 5,
                    subjectCounts = mapOf(
                        "物理" to 4,
                        "数学" to 6,
                    ),
                    monthlyNewCounts = listOf(
                        MonthlyCount(2025, 11, 2),
                        MonthlyCount(2026, 1, 3),
                        MonthlyCount(2026, 4, 5),
                    ),
                ),
                isLoading = false,
            ),
        )

        composeRule.onNodeWithContentDescription("饼图：数学 6，物理 4")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("stats-list").performScrollToIndex(7)
        composeRule.onNodeWithContentDescription(
            "趋势图：2025年11月 2，2025年12月 0，2026年1月 3，" +
                "2026年2月 0，2026年3月 0，2026年4月 5",
        ).performScrollTo().assertIsDisplayed()
    }

    private fun setStatsContent(state: StatsUiState) {
        composeRule.setContent {
            BanduTijiTheme {
                StatsScreen(
                    uiState = state,
                    onAction = {},
                )
            }
        }
    }
}
