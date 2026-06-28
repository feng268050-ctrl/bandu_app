package com.bandu.tiji.feature.capture

import com.bandu.tiji.core.model.id.CollectionId
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
class CaptureDraftCleanupTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `cancel cleans temporary draft and navigates back`() = runTest {
        val draftFiles = RecordingDraftFiles()
        val viewModel = CaptureViewModel(
            draftFiles = draftFiles,
            uuidGenerator = FixedUuidGenerator("draft-cancel"),
        )

        viewModel.onAction(CaptureAction.ImageSelected("content://capture/source"))
        viewModel.onAction(CaptureAction.Cancel)
        advanceUntilIdle()

        assertThat(draftFiles.cleanedDrafts).containsExactly("draft-cancel")
        assertThat(viewModel.effects.first()).isEqualTo(CaptureEffect.NavigateBack)
    }

    @Test
    fun `processing failure cleans temporary draft`() = runTest {
        val draftFiles = RecordingDraftFiles()
        val viewModel = CaptureViewModel(
            imageProcessor = RecordingImageProcessor(failure = IllegalStateException("bad")),
            draftFiles = draftFiles,
            uuidGenerator = FixedUuidGenerator("draft-failure"),
        )

        viewModel.onAction(CaptureAction.ImageSelected("content://capture/source"))
        viewModel.onAction(CaptureAction.ConfirmCrop)
        advanceUntilIdle()

        assertThat(draftFiles.cleanedDrafts).containsExactly("draft-failure")
        assertThat(viewModel.processedImage("draft-failure")).isNull()
    }

    @Test
    fun `successful save marks draft saved and does not clean processed image`() = runTest {
        val draftFiles = RecordingDraftFiles()
        val repository = FakeErrorItemRepository()
        val viewModel = reviewedViewModel(
            repository = repository,
            draftFiles = draftFiles,
        )
        viewModel.onAction(
            CaptureAction.UpdateReviewDraft(
                viewModel.uiState.value.reviewDraft!!.copy(collectionId = CollectionId("collection-1")),
            ),
        )

        viewModel.onAction(CaptureAction.SaveReview)
        advanceUntilIdle()

        assertThat(draftFiles.savedDrafts).containsExactly("draft-clean-save")
        assertThat(draftFiles.cleanedDrafts).isEmpty()
        assertThat(viewModel.processedImage("draft-clean-save")).isNotNull()
    }

    private suspend fun TestScope.reviewedViewModel(
        repository: FakeErrorItemRepository,
        draftFiles: RecordingDraftFiles,
    ): CaptureViewModel {
        val gateway = FakeAiTutorGateway().apply {
            enqueueAnalyze(CallScript.Return(analyzedQuestion()))
        }
        return CaptureViewModel(
            imageProcessor = RecordingImageProcessor(processed = processedImage()),
            aiGateway = gateway,
            errorItemRepository = repository,
            draftFiles = draftFiles,
            uuidGenerator = FixedUuidGenerator("draft-clean-save"),
        ).also { viewModel ->
            viewModel.onAction(CaptureAction.ImageSelected("content://capture/source"))
            viewModel.onAction(CaptureAction.ConfirmCrop)
            advanceUntilIdle()
        }
    }
}

private class RecordingDraftFiles : CaptureDraftFiles {
    val cleanedDrafts = mutableListOf<String>()
    val savedDrafts = mutableListOf<String>()

    override suspend fun cleanupDraft(draftId: String) {
        cleanedDrafts += draftId
    }

    override suspend fun markSaved(draftId: String) {
        savedDrafts += draftId
    }
}
