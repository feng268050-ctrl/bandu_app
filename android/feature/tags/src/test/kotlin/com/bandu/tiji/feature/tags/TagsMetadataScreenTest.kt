package com.bandu.tiji.feature.tags

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.bandu.tiji.core.designsystem.theme.BanduTijiTheme
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h800dp")
class TagsMetadataScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `standard tag shows level code and linked error item count`() {
        val child = tagNode(
            id = "linear",
            name = "一次函数",
            subject = "数学",
            code = "MATH-ALG-001",
            count = 7,
        )
        val root = tagNode(
            id = "algebra",
            name = "代数",
            subject = "数学",
            code = "MATH-ALG",
            count = 12,
            children = listOf(child),
        )
        composeRule.setContent {
            BanduTijiTheme {
                TagsScreen(
                    uiState = TagsUiState(
                        tree = listOf(root),
                        expandedTagIds = setOf(root.tag.id),
                        isLoading = false,
                    ),
                    onAction = {},
                )
            }
        }

        composeRule.onNodeWithText("第 1 层 · 标准标签").assertIsDisplayed()
        composeRule.onNodeWithText("编码：MATH-ALG").assertIsDisplayed()
        composeRule.onNodeWithText("关联错题：12 道").assertIsDisplayed()
        composeRule.onNodeWithText("第 2 层 · 标准标签").assertIsDisplayed()
        composeRule.onNodeWithText("编码：MATH-ALG-001").assertIsDisplayed()
        composeRule.onNodeWithText("关联错题：7 道").assertIsDisplayed()

        val parentBounds = composeRule.onNodeWithTag("tags-node-algebra")
            .fetchSemanticsNode()
            .boundsInRoot
        val childBounds = composeRule.onNodeWithTag("tags-node-linear")
            .fetchSemanticsNode()
            .boundsInRoot
        assertThat(childBounds.left).isGreaterThan(parentBounds.left)
    }
}
