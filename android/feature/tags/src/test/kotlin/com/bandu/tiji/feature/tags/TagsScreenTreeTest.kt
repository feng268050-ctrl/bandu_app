package com.bandu.tiji.feature.tags

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.bandu.tiji.core.designsystem.theme.BanduTijiTheme
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h800dp")
class TagsScreenTreeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `nine subject tabs dispatch selection and mark current subject`() {
        val actions = mutableListOf<TagsAction>()
        setTagsContent(
            state = TagsUiState(isLoading = false),
            onAction = actions::add,
        )

        StandardSubjects.forEachIndexed { index, subject ->
            composeRule.onNodeWithTag("tags-subjects").performScrollToIndex(index)
            composeRule.onNodeWithText(subject).assertExists()
        }
        composeRule.onNodeWithTag("tags-subjects").performScrollToIndex(0)
        composeRule.onNodeWithTag("tags-subject-数学").assertIsSelected()
        composeRule.onNodeWithTag("tags-subject-物理").performClick()

        composeRule.runOnIdle {
            assertThat(actions).contains(TagsAction.SelectSubject("物理"))
        }
    }

    @Test
    fun `tree child becomes visible only after expanding its parent`() {
        val child = tagNode(id = "linear", name = "一次函数", subject = "数学")
        val root = tagNode(
            id = "function",
            name = "函数",
            subject = "数学",
            children = listOf(child),
        )
        var state by mutableStateOf(
            TagsUiState(
            tree = listOf(root),
            isLoading = false,
            ),
        )
        composeRule.setContent {
            BanduTijiTheme {
                TagsScreen(
                    uiState = state,
                    onAction = { action ->
                        if (action is TagsAction.ToggleExpanded) {
                            state = state.copy(expandedTagIds = setOf(action.tagId))
                        }
                    },
                )
            }
        }

        composeRule.onNodeWithText("函数").assertIsDisplayed()
        composeRule.onNodeWithText("一次函数").assertDoesNotExist()

        composeRule.onNodeWithTag("tags-toggle-function").performClick()
        composeRule.onNodeWithText("一次函数").assertIsDisplayed()
    }

    @Test
    fun `load error dispatches retry`() {
        val actions = mutableListOf<TagsAction>()
        setTagsContent(
            state = TagsUiState(
                isLoading = false,
                loadErrorMessage = "无法加载标签",
            ),
            onAction = actions::add,
        )

        composeRule.onNodeWithText("无法加载标签").assertIsDisplayed()
        composeRule.onNodeWithText("重试").performClick()

        composeRule.runOnIdle {
            assertThat(actions).containsExactly(TagsAction.Retry)
        }
    }

    private fun setTagsContent(
        state: TagsUiState,
        onAction: (TagsAction) -> Unit,
    ) {
        composeRule.setContent {
            BanduTijiTheme {
                TagsScreen(
                    uiState = state,
                    onAction = onAction,
                )
            }
        }
    }
}
