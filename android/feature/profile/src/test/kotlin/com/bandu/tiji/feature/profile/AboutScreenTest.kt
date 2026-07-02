package com.bandu.tiji.feature.profile

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
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
@Config(sdk = [35], qualifiers = "w360dp-h800dp")
class AboutScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `about page shows required product disclosures`() {
        composeRule.setContent {
            BanduTijiTheme {
                AboutScreen(
                    aboutInfo = AboutInfo(versionName = "1.0.0"),
                    onBack = {},
                )
            }
        }

        listOf(
            "伴读题集",
            "1.0.0",
            "隐私说明",
            "开源许可",
            "图标来源",
        ).forEach { text ->
            composeRule.onNodeWithText(text, substring = true).assertExists()
        }
    }

    @Test
    fun `about page dispatches remote adb actions`() {
        val actions = mutableListOf<ProfileAction>()
        composeRule.setContent {
            BanduTijiTheme {
                AboutScreen(
                    aboutInfo = AboutInfo(versionName = "1.0.0"),
                    remoteAdbDebug = RemoteAdbDebugUiState(isSupported = true),
                    onBack = {},
                    onAction = actions::add,
                )
            }
        }

        assertThat(
            composeRule.onAllNodesWithText("ADB 远程调试").fetchSemanticsNodes(),
        ).isEmpty()
        repeat(5) {
            composeRule.onNodeWithTag("about-version-value").performClick()
        }

        composeRule.runOnIdle {
            assertThat(actions).containsExactly(
                ProfileAction.RefreshRemoteAdbDebug,
                ProfileAction.RequestEnableRemoteAdbDebug,
            )
        }
    }

    @Test
    fun `remote adb dialog includes version and confirms enable`() {
        val actions = mutableListOf<ProfileAction>()
        composeRule.setContent {
            BanduTijiTheme {
                AboutScreen(
                    aboutInfo = AboutInfo(versionName = "1.0.0"),
                    remoteAdbDebug = RemoteAdbDebugUiState(
                        isSupported = true,
                        showEnableConfirmation = true,
                    ),
                    onBack = {},
                    onAction = actions::add,
                )
            }
        }

        composeRule.onNodeWithText("版本 1.0.0").assertExists()
        composeRule.onNodeWithText("当前版本：1.0.0").assertExists()
        composeRule.onNodeWithTag("remote-adb-debug-dialog").assertExists()
        composeRule.onNodeWithText("开启").performClick()

        composeRule.runOnIdle {
            assertThat(actions).containsExactly(
                ProfileAction.RefreshRemoteAdbDebug,
                ProfileAction.ConfirmEnableRemoteAdbDebug,
            )
        }
    }
}
