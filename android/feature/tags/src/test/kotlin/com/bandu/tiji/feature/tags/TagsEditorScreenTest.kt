package com.bandu.tiji.feature.tags

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
@Config(sdk = [35])
class TagsEditorScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `root and child create buttons dispatch their parent ids`() {
        val parent = tagNode(id = "parent", name = "代数", subject = "数学")
        val actions = mutableListOf<TagsAction>()
        setContent(
            state = TagsUiState(tree = listOf(parent), isLoading = false),
            onAction = actions::add,
        )

        composeRule.onNodeWithTag("tags-create-root").performClick()
        composeRule.onNodeWithTag("tags-create-child-parent").performClick()

        composeRule.runOnIdle {
            assertThat(actions).containsExactly(
                TagsAction.OpenCreate(null),
                TagsAction.OpenCreate(parent.tag.id),
            ).inOrder()
        }
    }

    @Test
    fun `create dialog displays duplicate error and dispatches cancel`() {
        val actions = mutableListOf<TagsAction>()
        setContent(
            state = TagsUiState(
                isLoading = false,
                editor = TagEditorState(
                    mode = TagEditorMode.Create(parentId = null),
                    name = "函数",
                    errorMessage = "同一位置已存在同名标签",
                ),
            ),
            onAction = actions::add,
        )

        composeRule.onNodeWithText("新建自定义标签").assertIsDisplayed()
        composeRule.onNodeWithText("同一位置已存在同名标签").assertIsDisplayed()
        composeRule.onNodeWithText("取消").performClick()

        composeRule.runOnIdle {
            assertThat(actions).contains(TagsAction.DismissEditor)
        }
    }

    private fun setContent(
        state: TagsUiState,
        onAction: (TagsAction) -> Unit,
    ) {
        composeRule.setContent {
            BanduTijiTheme {
                TagsScreen(uiState = state, onAction = onAction)
            }
        }
    }
}
