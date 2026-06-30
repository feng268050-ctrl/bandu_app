package com.bandu.tiji.feature.library

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.bandu.tiji.core.designsystem.theme.BanduTijiTheme
import com.bandu.tiji.core.model.collection.CollectionSummary
import com.bandu.tiji.core.model.id.CollectionId
import com.google.common.truth.Truth.assertThat
import java.time.ZoneOffset
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CollectionListScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `collection cards show name count and updated time`() {
        setContent(
            CollectionListUiState(
                collections = listOf(collection()),
                isLoading = false,
            ),
        )

        composeRule.onNodeWithText("期中复习").assertIsDisplayed()
        composeRule.onNodeWithText("12 道错题").assertIsDisplayed()
        composeRule.onNodeWithText("最近更新", substring = true).assertIsDisplayed()
        assertThat(formatCollectionUpdatedAt(0L, ZoneOffset.UTC)).isEqualTo("1970-01-01 00:00")
    }

    @Test
    fun `collection card and retry dispatch actions`() {
        val actions = mutableListOf<CollectionListAction>()
        var state by mutableStateOf(
            CollectionListUiState(
                collections = listOf(collection()),
                isLoading = false,
            ),
        )
        composeRule.setContent {
            BanduTijiTheme {
                CollectionListScreen(
                    uiState = state,
                    onAction = actions::add,
                )
            }
        }
        composeRule.onNodeWithText("期中复习").performClick()

        composeRule.runOnIdle {
            state = CollectionListUiState(
                isLoading = false,
                errorMessage = "无法加载题集",
            )
        }
        composeRule.onNodeWithText("重试").performClick()

        composeRule.runOnIdle {
            assertThat(actions).containsExactly(
                CollectionListAction.OpenCollection(CollectionId("collection-1")),
                CollectionListAction.Retry,
            ).inOrder()
        }
    }

    @Test
    fun `back navigation dispatches callback`() {
        var backCount = 0
        composeRule.setContent {
            BanduTijiTheme {
                CollectionListScreen(
                    uiState = CollectionListUiState(isLoading = false),
                    onAction = {},
                    onBack = { backCount += 1 },
                )
            }
        }

        composeRule.onNodeWithText("返回").performClick()
        composeRule.runOnIdle {
            assertThat(backCount).isEqualTo(1)
        }
    }

    private fun setContent(uiState: CollectionListUiState) {
        composeRule.setContent {
            BanduTijiTheme {
                CollectionListScreen(uiState = uiState, onAction = {})
            }
        }
    }

    private fun collection() =
        CollectionSummary(
            id = CollectionId("collection-1"),
            name = "期中复习",
            errorItemCount = 12,
            updatedAtEpochMillis = 1_700_000_000_000L,
        )
}
