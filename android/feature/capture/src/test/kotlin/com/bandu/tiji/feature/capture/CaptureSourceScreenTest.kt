package com.bandu.tiji.feature.capture

import androidx.compose.ui.test.junit4.v2.createComposeRule
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
class CaptureSourceScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `source screen exposes only camera and photo picker`() {
        val actions = mutableListOf<CaptureAction>()
        composeRule.setContent {
            BanduTijiTheme {
                CaptureSourceScreen(onAction = actions::add)
            }
        }

        composeRule.onNodeWithTag(CAMERA_SOURCE_TAG).performClick()
        composeRule.onNodeWithTag(PHOTO_SOURCE_TAG).performClick()

        composeRule.runOnIdle {
            assertThat(actions).containsExactly(
                CaptureAction.ChooseCamera,
                CaptureAction.ChoosePhoto,
            ).inOrder()
        }
        composeRule.onNodeWithText("纯文本", substring = true).assertDoesNotExist()
        composeRule.onNodeWithText("文件", substring = true).assertDoesNotExist()
        composeRule.onNodeWithText("批量导入", substring = true).assertDoesNotExist()
    }
}
