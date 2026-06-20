package com.bandu.tiji.feature.library

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.bandu.tiji.core.designsystem.theme.BanduTijiTheme
import com.bandu.tiji.core.testing.fake.FakeCollectionRepository
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LibraryRoutesTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `all routes expose their state to independent screen content`() {
        composeRule.setContent {
            BanduTijiTheme {
                ErrorItemListRoute(ErrorItemListUiState(), {}) { _, _ ->
                    Text("错题路由")
                }
                ErrorItemDetailRoute(ErrorItemDetailUiState(), {}) { _, _ ->
                    Text("详情路由")
                }
            }
        }

        composeRule.onNodeWithText("错题路由").assertIsDisplayed()
        composeRule.onNodeWithText("详情路由").assertIsDisplayed()
    }

    @Test
    fun `collection route binds view model state to screen`() {
        val viewModel = CollectionListViewModel(FakeCollectionRepository())
        composeRule.setContent {
            BanduTijiTheme {
                CollectionListRoute(viewModel = viewModel, onNavigate = {})
            }
        }

        composeRule.onNodeWithText("还没有题集").assertIsDisplayed()
    }
}
