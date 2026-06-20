package com.bandu.tiji.feature.library

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.bandu.tiji.core.designsystem.theme.BanduTijiTheme
import com.bandu.tiji.core.testing.fake.FakeCollectionRepository
import com.bandu.tiji.core.testing.fake.FakeErrorItemRepository
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
    fun `collection route binds view model state to screen`() {
        val viewModel = CollectionListViewModel(FakeCollectionRepository())
        composeRule.setContent {
            BanduTijiTheme {
                CollectionListRoute(viewModel = viewModel, onNavigate = {})
            }
        }

        composeRule.onNodeWithText("还没有题集").assertIsDisplayed()
    }

    @Test
    fun `error item route binds paging data`() {
        val viewModel = ErrorItemListViewModel(FakeErrorItemRepository())
        composeRule.setContent {
            BanduTijiTheme {
                ErrorItemListRoute(viewModel = viewModel, onNavigate = {})
            }
        }

        composeRule.onNodeWithText("没有符合条件的错题").assertIsDisplayed()
    }
}
