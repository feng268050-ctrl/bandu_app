package com.bandu.tiji.feature.tags

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.bandu.tiji.core.designsystem.theme.BanduTijiTheme
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h800dp")
class TagsAccessibilityTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `empty subject explains that custom tags can be created`() {
        setContent(TagsUiState(isLoading = false))

        composeRule.onNodeWithText("该学科暂无标签").assertIsDisplayed()
        composeRule.onNodeWithText("可以创建自定义标签整理错题").assertIsDisplayed()
    }

    @Test
    fun `large expanded tree keeps stable hierarchy and scrolls to last root`() {
        val tree = (0 until 100).map { index ->
            tagNode(
                id = "root-$index",
                name = "知识点 $index",
                subject = "数学",
                children = listOf(
                    tagNode(
                        id = "child-$index",
                        name = "子知识点 $index",
                        subject = "数学",
                    ),
                ),
            )
        }
        val flattened = flattenVisibleTags(
            tree = tree,
            expandedTagIds = tree.map { it.tag.id }.toSet(),
        )

        assertThat(flattened).hasSize(200)
        assertThat(flattened.first().depth).isEqualTo(0)
        assertThat(flattened[1].depth).isEqualTo(1)
        assertThat(flattened.last().node.tag.name).isEqualTo("子知识点 99")

        setContent(
            TagsUiState(
                tree = tree,
                isLoading = false,
            ),
        )
        composeRule.onNodeWithTag("tags-tree").performScrollToIndex(99)
        composeRule.onNodeWithText("知识点 99").assertIsDisplayed()
    }

    @Test
    fun `two hundred percent font keeps tag card within narrow page`() {
        val custom = tagNode(
            id = "custom",
            name = "需要重点复习的自定义知识点",
            subject = "数学",
            isSystem = false,
            code = "CUSTOM-001",
            count = 123,
        )
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(
                    density = density.density,
                    fontScale = 2f,
                ),
            ) {
                BanduTijiTheme {
                    TagsScreen(
                        uiState = TagsUiState(
                            tree = listOf(custom),
                            isLoading = false,
                        ),
                        onAction = {},
                        modifier = Modifier
                            .width(320.dp)
                            .testTag("tags-narrow-root"),
                    )
                }
            }
        }

        composeRule.onNodeWithText("需要重点复习的自定义知识点").assertIsDisplayed()
        composeRule.onNodeWithText("关联错题：123 道").assertIsDisplayed()
        val rootBounds = composeRule.onNodeWithTag("tags-narrow-root")
            .fetchSemanticsNode().boundsInRoot
        val cardBounds = composeRule.onNodeWithTag("tags-node-custom")
            .fetchSemanticsNode().boundsInRoot
        assertThat(cardBounds.left).isAtLeast(rootBounds.left)
        assertThat(cardBounds.right).isAtMost(rootBounds.right)
    }

    private fun setContent(state: TagsUiState) {
        composeRule.setContent {
            BanduTijiTheme {
                TagsScreen(
                    uiState = state,
                    onAction = {},
                )
            }
        }
    }
}
