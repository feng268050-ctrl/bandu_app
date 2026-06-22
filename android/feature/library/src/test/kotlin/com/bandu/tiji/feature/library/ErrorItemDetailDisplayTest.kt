package com.bandu.tiji.feature.library

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToIndex
import com.bandu.tiji.core.designsystem.theme.BanduTijiTheme
import com.bandu.tiji.core.model.enums.MasteryLevel
import com.bandu.tiji.core.model.enums.MistakeStatus
import com.bandu.tiji.core.model.enums.PaperLevel
import com.bandu.tiji.core.model.erroritem.ErrorItem
import com.bandu.tiji.core.model.id.CollectionId
import com.bandu.tiji.core.model.id.ErrorItemId
import com.bandu.tiji.core.model.id.TagId
import com.bandu.tiji.core.model.tag.TagSummary
import com.bandu.tiji.core.testing.coroutines.MainDispatcherRule
import com.bandu.tiji.core.testing.fake.FakeErrorItemRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
class ErrorItemDetailViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `detail observes requested item offline`() = runTest {
        val item = detailItem()
        val viewModel = ErrorItemDetailViewModel(
            FakeErrorItemRepository(listOf(item)),
            item.id,
        )
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.item).isEqualTo(item)
        assertThat(viewModel.uiState.value.isLoading).isFalse()
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h800dp")
class ErrorItemDetailDisplayTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `detail shows all saved fields`() {
        composeRule.setContent {
            BanduTijiTheme {
                ErrorItemDetailScreen(
                    uiState = ErrorItemDetailUiState(
                        item = detailItem(),
                        isLoading = false,
                    ),
                    onAction = {},
                    markdownRenderer = { markdown, modifier ->
                        Text(markdown, modifier)
                    },
                )
            }
        }

        listOf("题目", "函数题", "答案", "x = 1").forEach { text ->
            composeRule.onNodeWithText(text).assertIsDisplayed()
        }
        composeRule.onNodeWithTag("error-item-detail-list").performScrollToIndex(3)
        listOf("错误答案：x = 2", "错误分析：符号错误").forEach { text ->
            composeRule.onNodeWithText(text, substring = true)
                .assertIsDisplayed()
        }
        composeRule.onNodeWithTag("error-item-detail-list").performScrollToIndex(4)
        composeRule.onNodeWithText("一次方程").assertIsDisplayed()
        composeRule.onNodeWithTag("error-item-detail-list").performScrollToIndex(5)
        listOf("学科：数学", "年级/学期：八年级上", "试卷等级：A").forEach { text ->
            composeRule.onNodeWithText(text).assertIsDisplayed()
        }
        composeRule.onNodeWithTag("error-item-detail-list").performScrollToIndex(6)
        composeRule.onNodeWithText("重点复习").assertIsDisplayed()
    }
}

internal fun detailItem() =
    ErrorItem(
        id = ErrorItemId("error-1"),
        collectionId = CollectionId("collection-1"),
        image = null,
        questionText = "函数题",
        answerText = "x = 1",
        analysis = "移项",
        wrongAnswerText = "x = 2",
        mistakeStatus = MistakeStatus.WRONG_ATTEMPT,
        mistakeAnalysis = "符号错误",
        subject = "数学",
        tags = listOf(TagSummary(TagId("tag-1"), "一次方程", "数学", true)),
        gradeSemester = "八年级上",
        paperLevel = PaperLevel.A,
        notes = "重点复习",
        masteryLevel = MasteryLevel.REVIEWING,
        createdAtEpochMillis = 0L,
        updatedAtEpochMillis = 0L,
    )
