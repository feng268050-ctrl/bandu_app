package com.bandu.tiji.feature.stats

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.bandu.tiji.core.designsystem.theme.BanduTijiTheme
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
