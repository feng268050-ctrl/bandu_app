package com.bandu.tiji.feature.profile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import com.bandu.tiji.core.designsystem.theme.BanduTijiTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AiProviderScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `AI settings offers Gemini and OpenAI compatible but not Azure`() {
        composeRule.setContent {
            BanduTijiTheme {
                ProfileScreen(
                    uiState = ProfileUiState(currentSection = ProfileSection.AI),
                    onAction = {},
                )
            }
        }

        composeRule.onAllNodesWithText("Gemini").assertCountEquals(2)
        composeRule.onNodeWithText("OpenAI-compatible").assertIsDisplayed()
        composeRule.onNodeWithText("Azure").assertDoesNotExist()
    }
}
