package com.bandu.tiji.feature.capture

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import com.bandu.tiji.core.designsystem.theme.BanduTijiTheme
import com.bandu.tiji.core.model.collection.CollectionSummary
import com.bandu.tiji.core.model.enums.MistakeStatus
import com.bandu.tiji.core.model.enums.PaperLevel
import com.bandu.tiji.core.model.id.CollectionId
import com.bandu.tiji.core.model.id.TagId
import com.bandu.tiji.core.model.tag.TagSummary
import com.bandu.tiji.core.testing.coroutines.MainDispatcherRule
import com.bandu.tiji.core.testing.fake.CallScript
import com.bandu.tiji.core.testing.fake.FakeAiTutorGateway
import com.bandu.tiji.core.testing.id.FixedUuidGenerator
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
class CaptureReviewEditViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `review draft updates all editable fields`() = runTest {
        val viewModel = reviewedViewModel()
        val updated = CaptureReviewDraft(
            collectionId = CollectionId("collection-1"),
            subject = "物理",
            questionText = "题目改写",
            answerText = "答案改写",
            analysis = "解析改写",
            wrongAnswerText = "错误答案",
            mistakeStatus = MistakeStatus.NOT_ATTEMPTED,
            mistakeAnalysis = "看错条件",
            tagIds = listOf(TagId("tag-1"), TagId("tag-2")),
            gradeSemester = "八年级上",
            paperLevel = PaperLevel.A,
            notes = "需要复习",
        )

        viewModel.onAction(CaptureAction.UpdateReviewDraft(updated))

        assertThat(viewModel.uiState.value.reviewDraft).isEqualTo(updated)
    }

    @Test
    fun `review tags are limited to five and selected tags can be removed`() = runTest {
        val viewModel = reviewedViewModel()

        (1..5).forEach { index ->
            viewModel.onAction(CaptureAction.ToggleReviewTag(TagId("tag-$index")))
        }
        viewModel.onAction(CaptureAction.ToggleReviewTag(TagId("tag-6")))

        assertThat(viewModel.uiState.value.reviewDraft?.tagIds)
            .containsExactlyElementsIn((1..5).map { TagId("tag-$it") })
            .inOrder()

        viewModel.onAction(CaptureAction.ToggleReviewTag(TagId("tag-3")))
        viewModel.onAction(CaptureAction.ToggleReviewTag(TagId("tag-6")))

        assertThat(viewModel.uiState.value.reviewDraft?.tagIds)
            .containsExactly(
                TagId("tag-1"),
                TagId("tag-2"),
                TagId("tag-4"),
                TagId("tag-5"),
                TagId("tag-6"),
            )
            .inOrder()
    }

    private suspend fun TestScope.reviewedViewModel(): CaptureViewModel {
        val gateway = FakeAiTutorGateway().apply {
            enqueueAnalyze(CallScript.Return(analyzedQuestion()))
        }
        return CaptureViewModel(
            imageProcessor = RecordingImageProcessor(processed = processedImage()),
            aiGateway = gateway,
            uuidGenerator = FixedUuidGenerator("draft-review"),
        ).also { viewModel ->
            viewModel.onAction(CaptureAction.ImageSelected("content://capture/source"))
            viewModel.onAction(CaptureAction.ConfirmCrop)
            advanceUntilIdle()
        }
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h800dp")
class CaptureReviewScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `review screen exposes editable analyzed fields and selected tag count`() {
        val actions = mutableListOf<CaptureAction>()

        composeRule.setContent {
            BanduTijiTheme {
                CaptureReviewScreen(
                    uiState = CaptureUiState(
                        stage = CaptureStage.Reviewing("draft-1"),
                        reviewDraft = CaptureReviewDraft(
                            subject = "数学",
                            questionText = "原题",
                            answerText = "原答案",
                            analysis = "原解析",
                            tagIds = listOf(TagId("tag-1")),
                        ),
                        availableCollections = listOf(
                            CollectionSummary(CollectionId("collection-1"), "期中题集", 0, 1L),
                        ),
                        availableTags = listOf(
                            TagSummary(TagId("tag-1"), "方程", "数学", true),
                        ),
                    ),
                    onAction = actions::add,
                )
            }
        }

        composeRule.onNodeWithTag(REVIEW_SCREEN_TAG).assertIsDisplayed()
        composeRule.onNodeWithText("期中题集").assertIsDisplayed()
        composeRule.onNodeWithText("已选择 1/5").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(REVIEW_QUESTION_TAG).performScrollTo().performTextReplacement("新题目")

        composeRule.runOnIdle {
            assertThat(actions.last()).isEqualTo(
                CaptureAction.UpdateReviewDraft(
                    CaptureReviewDraft(
                        subject = "数学",
                        questionText = "新题目",
                        answerText = "原答案",
                        analysis = "原解析",
                        tagIds = listOf(TagId("tag-1")),
                    ),
                ),
            )
        }
    }
}
