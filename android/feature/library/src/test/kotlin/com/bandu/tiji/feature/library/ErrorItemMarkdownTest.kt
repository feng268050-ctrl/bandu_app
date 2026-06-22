package com.bandu.tiji.feature.library

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToIndex
import com.bandu.tiji.core.designsystem.theme.BanduTijiTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h800dp")
class ErrorItemMarkdownTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `question answer and analysis are delegated to rich text renderer`() {
        val item = detailItem().copy(
            questionText = "| x | y |\n|---|---|\n| 1 | 2 |",
            answerText = "\$x^2\$",
            analysis = "\$\$\\frac{1}{2}\$\$",
        )
        composeRule.setContent {
            BanduTijiTheme {
                ErrorItemDetailScreen(
                    uiState = ErrorItemDetailUiState(item = item, isLoading = false),
                    onAction = {},
                    markdownRenderer = { markdown, modifier ->
                        Text("rich:$markdown", modifier)
                    },
                )
            }
        }

        composeRule.onNodeWithTag("detail-markdown-题目").assertExists()
        composeRule.onNodeWithText("rich:| x | y |", substring = true).assertIsDisplayed()
        composeRule.onNodeWithTag("error-item-detail-list").performScrollToIndex(1)
        composeRule.onNodeWithText("rich:\$x^2\$").assertIsDisplayed()
        composeRule.onNodeWithTag("error-item-detail-list").performScrollToIndex(2)
        composeRule.onNodeWithText("rich:\$\$\\frac{1}{2}\$\$").assertIsDisplayed()
    }
}
