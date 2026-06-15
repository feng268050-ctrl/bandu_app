package com.bandu.tiji.core.designsystem.component

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.bandu.tiji.core.designsystem.theme.BanduTijiTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class BanduLayoutTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `page scaffold renders title actions and content`() {
        composeRule.setContent {
            BanduTijiTheme {
                BanduPageScaffold(
                    title = "错题详情",
                    actions = { Text("编辑") },
                ) {
                    Text("页面内容")
                }
            }
        }

        composeRule.onNodeWithText("错题详情").assertIsDisplayed()
        composeRule.onNodeWithText("编辑").assertIsDisplayed()
        composeRule.onNodeWithText("页面内容").assertIsDisplayed()
    }

    @Test
    fun `card renders grouped content`() {
        composeRule.setContent {
            BanduTijiTheme {
                BanduCard {
                    Text("题目")
                    Text("解答")
                }
            }
        }

        composeRule.onNodeWithText("题目").assertIsDisplayed()
        composeRule.onNodeWithText("解答").assertIsDisplayed()
    }
}
