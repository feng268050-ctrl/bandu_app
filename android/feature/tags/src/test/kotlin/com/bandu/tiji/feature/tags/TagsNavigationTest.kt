package com.bandu.tiji.feature.tags

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.bandu.tiji.core.designsystem.theme.BanduTijiTheme
import com.bandu.tiji.core.testing.coroutines.MainDispatcherRule
import com.bandu.tiji.core.testing.fake.FakeTagRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
class TagsNavigationViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `open tag emits filtered error item navigation effect`() = runTest {
        val node = tagNode("algebra", "代数", "数学")
        val viewModel = TagsViewModel(FakeTagRepository(listOf(node)))
        advanceUntilIdle()
        val effect = async { viewModel.effects.first() }

        viewModel.onAction(TagsAction.OpenTag(node.tag.id))

        assertThat(effect.await()).isEqualTo(
            TagsEffect.OpenFilteredErrorItems(node.tag.id),
        )
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class TagsNavigationScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `clicking tag card dispatches open action without toggling`() {
        val node = tagNode(
            id = "algebra",
            name = "代数",
            subject = "数学",
            children = listOf(tagNode("linear", "线性方程", "数学")),
        )
        val actions = mutableListOf<TagsAction>()
        composeRule.setContent {
            BanduTijiTheme {
                TagsScreen(
                    uiState = TagsUiState(
                        tree = listOf(node),
                        isLoading = false,
                    ),
                    onAction = actions::add,
                )
            }
        }

        composeRule.onNodeWithTag("tags-node-algebra").performClick()

        composeRule.runOnIdle {
            assertThat(actions).containsExactly(TagsAction.OpenTag(node.tag.id))
        }
    }
}
