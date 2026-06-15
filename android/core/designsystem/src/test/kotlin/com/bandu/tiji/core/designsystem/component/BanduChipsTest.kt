package com.bandu.tiji.core.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.bandu.tiji.core.designsystem.theme.BanduColors
import com.bandu.tiji.core.designsystem.theme.BanduTijiTheme
import com.bandu.tiji.core.model.enums.MasteryLevel
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class BanduChipsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `mastery badge maps every domain state to product label and color`() {
        assertThat(masteryBadgeStyle(MasteryLevel.NEW))
            .isEqualTo(
                MasteryBadgeStyle(
                    label = "未掌握",
                    containerColor = BanduColors.DangerContainer,
                    contentColor = BanduColors.Danger,
                ),
            )
        assertThat(masteryBadgeStyle(MasteryLevel.REVIEWING).label).isEqualTo("复习中")
        assertThat(masteryBadgeStyle(MasteryLevel.MASTERED))
            .isEqualTo(
                MasteryBadgeStyle(
                    label = "已掌握",
                    containerColor = BanduColors.SuccessContainer,
                    contentColor = BanduColors.Success,
                ),
            )
    }

    @Test
    fun `all mastery badge states expose mapped semantics`() {
        composeRule.setContent {
            BanduTijiTheme {
                Column {
                    MasteryLevel.entries.forEach { level ->
                        BanduMasteryBadge(level)
                    }
                }
            }
        }

        composeRule.onNodeWithContentDescription("掌握状态：未掌握").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("掌握状态：复习中").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("掌握状态：已掌握").assertIsDisplayed()
    }

    @Test
    fun `tag and filter chips expose selection and clicks`() {
        var tagClicks = 0
        var filterClicks = 0
        composeRule.setContent {
            BanduTijiTheme {
                Column {
                    BanduTagChip(
                        label = "二次函数",
                        selected = true,
                        onClick = { tagClicks += 1 },
                    )
                    BanduFilterChip(
                        label = "最近 7 天",
                        selected = false,
                        onClick = { filterClicks += 1 },
                    )
                }
            }
        }

        composeRule.onNodeWithText("二次函数").assertIsSelected().performClick()
        composeRule.onNodeWithText("最近 7 天").assertIsNotSelected().performClick()
        composeRule.runOnIdle {
            assertThat(tagClicks).isEqualTo(1)
            assertThat(filterClicks).isEqualTo(1)
        }
    }

    @Test
    fun `paging item announces loading state`() {
        composeRule.setContent {
            BanduTijiTheme {
                BanduPagingLoadingItem()
            }
        }

        composeRule.onNodeWithContentDescription("正在加载更多").assertIsDisplayed()
        composeRule.onNodeWithText("正在加载更多").assertIsDisplayed()
    }
}
