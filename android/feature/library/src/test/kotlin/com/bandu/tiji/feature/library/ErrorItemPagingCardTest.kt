package com.bandu.tiji.feature.library

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.bandu.tiji.core.designsystem.theme.BanduTijiTheme
import com.bandu.tiji.core.model.enums.MasteryLevel
import com.bandu.tiji.core.model.erroritem.ErrorItemSummary
import com.bandu.tiji.core.model.id.CollectionId
import com.bandu.tiji.core.model.id.ErrorItemId
import com.bandu.tiji.core.model.id.TagId
import com.bandu.tiji.core.model.tag.TagSummary
import com.bandu.tiji.core.testing.coroutines.MainDispatcherRule
import com.bandu.tiji.core.testing.fake.FakeErrorItemRepository
import com.google.common.truth.Truth.assertThat
import java.time.ZoneOffset
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h800dp")
class ErrorItemPagingCardTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `card snapshot exposes thumbnail summary metadata tags time and mastery`() {
        var opened = false
        var requestedThumbnailPath: String? = null
        composeRule.setContent {
            BanduTijiTheme {
                LazyColumn {
                    item {
                        ErrorItemCard(
                            item = summary(),
                            onClick = { opened = true },
                            thumbnailModel = { path ->
                                requestedThumbnailPath = path
                                path
                            },
                        )
                    }
                }
            }
        }
        listOf(
            "求方程 x + 1 = 2",
            "代数题集",
            "一次方程 · 移项",
            "1970-01-01",
            "掌握状态：复习中",
        ).forEach { text ->
            if (text.startsWith("掌握状态")) {
                composeRule.onNodeWithContentDescription(text)
                    .performScrollTo()
                    .assertIsDisplayed()
            } else {
                composeRule.onNodeWithText(text)
                    .performScrollTo()
                    .assertIsDisplayed()
            }
        }

        composeRule.onNodeWithText("求方程 x + 1 = 2").performClick()
        composeRule.runOnIdle {
            assertThat(opened).isTrue()
            assertThat(requestedThumbnailPath).isEqualTo("thumbs/error-1.webp")
        }
        assertThat(formatErrorItemCreatedAt(0L, ZoneOffset.UTC)).isEqualTo("1970-01-01")
    }

    private fun summary() =
        ErrorItemSummary(
            id = ErrorItemId("error-1"),
            collectionId = CollectionId("collection-1"),
            collectionName = "代数题集",
            thumbnailPath = "thumbs/error-1.webp",
            questionPreview = "求方程 x + 1 = 2",
            tags = listOf(
                TagSummary(TagId("tag-1"), "一次方程", "数学", true),
                TagSummary(TagId("tag-2"), "移项", "数学", false),
            ),
            masteryLevel = MasteryLevel.REVIEWING,
            createdAtEpochMillis = 0L,
        )
}

@OptIn(ExperimentalCoroutinesApi::class)
class ErrorItemListViewModelPagingTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `paging uses required page and prefetch sizes`() = runTest {
        val repository = FakeErrorItemRepository()
        val viewModel = ErrorItemListViewModel(repository)

        viewModel.pagingData

        assertThat(ErrorItemListViewModel.PAGE_SIZE).isEqualTo(30)
        assertThat(ErrorItemListViewModel.PREFETCH_DISTANCE).isEqualTo(10)
    }
}
