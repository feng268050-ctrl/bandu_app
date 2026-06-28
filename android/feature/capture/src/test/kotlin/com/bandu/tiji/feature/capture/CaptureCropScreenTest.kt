package com.bandu.tiji.feature.capture

import androidx.compose.ui.test.assertIsDisplayed
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
class CaptureCropScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `crop screen shows source rotation and dispatches crop actions`() {
        val actions = mutableListOf<CaptureAction>()

        composeRule.setContent {
            BanduTijiTheme {
                CaptureCropScreen(
                    stage = CaptureStage.Crop(
                        draftId = "draft-1",
                        tempUri = "content://capture/image",
                        rotationDegrees = 90,
                    ),
                    onAction = actions::add,
                )
            }
        }

        composeRule.onNodeWithTag(CROP_PREVIEW_TAG).assertIsDisplayed()
        composeRule.onNodeWithText("旋转：90°", substring = true).assertIsDisplayed()
        composeRule.onNodeWithTag(CROP_ROTATE_TAG).performClick()
        composeRule.onNodeWithTag(CROP_CONFIRM_TAG).performClick()
        composeRule.onNodeWithTag(CROP_CANCEL_TAG).performClick()

        composeRule.runOnIdle {
            assertThat(actions).containsExactly(
                CaptureAction.RotateCropClockwise,
                CaptureAction.ConfirmCrop,
                CaptureAction.Cancel,
            ).inOrder()
        }
    }

    @Test
    fun `crop rotation helper wraps quarter turns`() {
        val stage = CaptureStage.Crop("draft-1", "content://capture/image", 270)

        assertThat(stage.rotatedClockwise().rotationDegrees).isEqualTo(0)
    }
}
