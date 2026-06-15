package com.bandu.tiji.core.designsystem.component

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.bandu.tiji.core.designsystem.theme.BanduTijiTheme
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class BanduDangerConfirmationTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `confirm stays disabled until required text exactly matches`() {
        var confirmationText by mutableStateOf("")
        var confirmCount = 0

        composeRule.setContent {
            BanduTijiTheme {
                BanduDangerConfirmationDialog(
                    title = "恢复出厂设置",
                    message = "此操作会删除全部本地数据。",
                    requiredConfirmationText = "恢复出厂设置",
                    confirmationText = confirmationText,
                    onConfirmationTextChange = { confirmationText = it },
                    onConfirm = { confirmCount += 1 },
                    onDismiss = {},
                )
            }
        }

        composeRule.onNodeWithText("确认").assertIsNotEnabled()
        composeRule.onNodeWithText("输入“恢复出厂设置”以确认")
            .performTextInput("恢复")
        composeRule.onNodeWithText("确认").assertIsNotEnabled()
        composeRule.onNodeWithText("确认文本不匹配")
            .assertExists()
        composeRule.onNodeWithText("输入“恢复出厂设置”以确认")
            .performTextInput("出厂设置")
        composeRule.onNodeWithText("确认").assertIsEnabled().performClick()

        composeRule.runOnIdle {
            assertThat(confirmCount).isEqualTo(1)
        }
    }

    @Test
    fun `dialog without required text supports confirm and dismiss`() {
        var confirmCount = 0
        var dismissCount = 0

        composeRule.setContent {
            BanduTijiTheme {
                BanduDangerConfirmationDialog(
                    title = "删除错题",
                    message = "删除后无法恢复。",
                    confirmLabel = "删除",
                    onConfirm = { confirmCount += 1 },
                    onDismiss = { dismissCount += 1 },
                )
            }
        }

        composeRule.onNodeWithText("删除").assertIsEnabled().performClick()
        composeRule.onNodeWithText("取消").performClick()

        composeRule.runOnIdle {
            assertThat(confirmCount).isEqualTo(1)
            assertThat(dismissCount).isEqualTo(1)
        }
    }
}
