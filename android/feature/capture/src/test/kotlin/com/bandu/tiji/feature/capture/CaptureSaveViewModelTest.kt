package com.bandu.tiji.feature.capture

import com.bandu.tiji.core.model.enums.MistakeStatus
import com.bandu.tiji.core.model.erroritem.ErrorItem
import com.bandu.tiji.core.model.erroritem.ErrorItemDraft
import com.bandu.tiji.core.model.erroritem.ErrorItemPatch
import com.bandu.tiji.core.model.erroritem.ErrorItemQuery
import com.bandu.tiji.core.model.erroritem.ErrorItemSummary
import com.bandu.tiji.core.model.id.CollectionId
import com.bandu.tiji.core.model.id.ErrorItemId
import com.bandu.tiji.core.model.id.TagId
import com.bandu.tiji.core.model.navigation.NavigationIntent
import com.bandu.tiji.core.testing.coroutines.MainDispatcherRule
import com.bandu.tiji.core.testing.fake.CallScript
import com.bandu.tiji.core.testing.fake.FakeAiTutorGateway
import com.bandu.tiji.core.testing.fake.FakeErrorItemRepository
import com.bandu.tiji.core.testing.id.FixedUuidGenerator
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import androidx.paging.PagingSource
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

    @Test
    fun `repeated save clicks create only one error item`() = runTest {
        val repository = SlowCreateErrorItemRepository()
        val viewModel = reviewedViewModel(repository)
        viewModel.onAction(CaptureAction.UpdateReviewDraft(reviewDraft()))

        viewModel.onAction(CaptureAction.SaveReview)
        viewModel.onAction(CaptureAction.SaveReview)
        advanceUntilIdle()

        assertThat(repository.createdDrafts).hasSize(1)
    }

    private suspend fun TestScope.reviewedViewModel(
        repository: com.bandu.tiji.domain.repository.ErrorItemRepository,
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

private class SlowCreateErrorItemRepository : com.bandu.tiji.domain.repository.ErrorItemRepository {
    val createdDrafts = mutableListOf<ErrorItemDraft>()

    override fun page(query: ErrorItemQuery): PagingSource<Int, ErrorItemSummary> =
        error("Not used")

    override fun observe(id: ErrorItemId): Flow<ErrorItem?> =
        error("Not used")

    override suspend fun create(draft: ErrorItemDraft): ErrorItemId {
        delay(100)
        createdDrafts += draft
        return ErrorItemId("error-slow")
    }

    override suspend fun update(id: ErrorItemId, patch: ErrorItemPatch) =
        error("Not used")

    override suspend fun delete(ids: Set<ErrorItemId>) =
        error("Not used")
}
