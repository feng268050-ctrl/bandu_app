package com.bandu.tiji.core.designsystem.component

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.bandu.tiji.core.designsystem.theme.BanduTijiTheme
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class BanduStatesTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `empty state renders optional action`() {
        composeRule.setContent {
            BanduTijiTheme {
                BanduEmptyState(
                    title = "还没有错题",
                    description = "拍照添加第一道错题",
                    actionLabel = "去添加",
                    onAction = {},
                )
            }
        }

        composeRule.onNodeWithText("还没有错题").assertIsDisplayed()
        composeRule.onNodeWithText("拍照添加第一道错题").assertIsDisplayed()
        composeRule.onNodeWithText("去添加").assertIsDisplayed()
    }

    @Test
    fun `loading state exposes progress description`() {
        composeRule.setContent {
            BanduTijiTheme {
                BanduLoadingState(message = "正在分析图片")
            }
        }

        composeRule.onNodeWithContentDescription("正在分析图片").assertIsDisplayed()
        composeRule.onNodeWithText("正在分析图片").assertIsDisplayed()
    }

    @Test
    fun `error state invokes retry`() {
        var retryCount = 0
        composeRule.setContent {
            BanduTijiTheme {
                BanduErrorState(
                    message = "网络不可用",
                    onRetry = { retryCount += 1 },
                )
            }
        }

        composeRule.onNodeWithText("加载失败").assertIsDisplayed()
        composeRule.onNodeWithText("网络不可用").assertIsDisplayed()
        composeRule.onNodeWithText("重试").performClick()

        composeRule.runOnIdle {
            assertThat(retryCount).isEqualTo(1)
        }
    }
}
