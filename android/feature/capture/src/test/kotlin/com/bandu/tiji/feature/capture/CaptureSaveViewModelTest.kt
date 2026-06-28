package com.bandu.tiji.feature.capture

import com.bandu.tiji.core.model.enums.MistakeStatus
import com.bandu.tiji.core.model.id.CollectionId
import com.bandu.tiji.core.model.id.TagId
import com.bandu.tiji.core.model.navigation.NavigationIntent
import com.bandu.tiji.core.testing.coroutines.MainDispatcherRule
import com.bandu.tiji.core.testing.fake.CallScript
import com.bandu.tiji.core.testing.fake.FakeAiTutorGateway
import com.bandu.tiji.core.testing.fake.FakeErrorItemRepository
import com.bandu.tiji.core.testing.id.FixedUuidGenerator
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CaptureSaveViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `save requires collection before creating error item`() = runTest {
        val repository = FakeErrorItemRepository()
        val viewModel = reviewedViewModel(repository)

        viewModel.onAction(CaptureAction.SaveReview)
        advanceUntilIdle()

        assertThat(repository.createdDrafts).isEmpty()
        assertThat(viewModel.uiState.value.errorMessage)
            .isEqualTo("请选择题集后保存。")
    }

    @Test
    fun `save creates error item and navigates to detail`() = runTest {
        val repository = FakeErrorItemRepository()
        val viewModel = reviewedViewModel(repository)
        viewModel.onAction(CaptureAction.UpdateReviewDraft(reviewDraft()))

        viewModel.onAction(CaptureAction.SaveReview)
        advanceUntilIdle()

        assertThat(repository.createdDrafts).hasSize(1)
        val draft = repository.createdDrafts.single()
        assertThat(draft.collectionId).isEqualTo(CollectionId("collection-1"))
        assertThat(draft.image).isEqualTo(processedImage().storedImage)
        assertThat(draft.questionText).isEqualTo("保存题目")
        assertThat(draft.tagIds).containsExactly(TagId("tag-1"))
        assertThat(viewModel.effects.first())
            .isEqualTo(CaptureEffect.Navigate(NavigationIntent.OpenErrorItem("error-item-1")))
    }

    private suspend fun TestScope.reviewedViewModel(
        repository: FakeErrorItemRepository,
    ): CaptureViewModel {
        val gateway = FakeAiTutorGateway().apply {
            enqueueAnalyze(CallScript.Return(analyzedQuestion()))
        }
        return CaptureViewModel(
            imageProcessor = RecordingImageProcessor(processed = processedImage()),
            aiGateway = gateway,
            errorItemRepository = repository,
            uuidGenerator = FixedUuidGenerator("draft-save"),
        ).also { viewModel ->
            viewModel.onAction(CaptureAction.ImageSelected("content://capture/source"))
            viewModel.onAction(CaptureAction.ConfirmCrop)
            advanceUntilIdle()
        }
    }

    private fun reviewDraft() =
        CaptureReviewDraft(
            collectionId = CollectionId("collection-1"),
            subject = "数学",
            questionText = "保存题目",
            answerText = "保存答案",
            analysis = "保存解析",
            wrongAnswerText = "错误答案",
            mistakeStatus = MistakeStatus.WRONG_ATTEMPT,
            mistakeAnalysis = "计算错误",
            tagIds = listOf(TagId("tag-1")),
        )
}
