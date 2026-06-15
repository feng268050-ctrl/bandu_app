package com.bandu.tiji

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AppGlobalStatusTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `unhandled error is shown by the global snackbar host`() {
        composeRule.setContent {
            BanduTijiApp(
                uiState = AppUiState(unhandledError = "保存失败，请重试"),
            )
        }

        composeRule.onNodeWithText("保存失败，请重试").assertIsDisplayed()
    }

    @Test
    fun `active migration is always visible above routed content`() {
        composeRule.setContent {
            BanduTijiApp(
                uiState = AppUiState(
                    migration = MigrationUiState(
                        progress = 0.4f,
                        statusText = "正在迁移数据 40%",
                    ),
                ),
            )
        }

        composeRule.onNodeWithText("正在迁移数据 40%").assertIsDisplayed()
        composeRule.onNodeWithText("首页 - 伴读题集").assertIsDisplayed()
    }

    @Test
    fun `migration state rejects invalid progress and blank status`() {
        assertThat(
            runCatching {
                MigrationUiState(progress = 1.1f, statusText = "迁移中")
            }.exceptionOrNull(),
        ).isInstanceOf(IllegalArgumentException::class.java)
        assertThat(
            runCatching {
                MigrationUiState(progress = 0f, statusText = " ")
            }.exceptionOrNull(),
        ).isInstanceOf(IllegalArgumentException::class.java)
    }
}
