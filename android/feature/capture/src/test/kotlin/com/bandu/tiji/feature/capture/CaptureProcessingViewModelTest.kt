package com.bandu.tiji.feature.capture

import com.bandu.tiji.core.model.erroritem.StoredImage
import com.bandu.tiji.core.testing.coroutines.MainDispatcherRule
import com.bandu.tiji.core.testing.id.FixedUuidGenerator
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CaptureProcessingViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `confirm crop processes image updates progress and shows minimum quality warning`() = runTest {
        val processor = RecordingImageProcessor(
            processed = processedImage(reachedMinimumQuality = true),
            progressValues = listOf(15, 60, 100),
        )
        val viewModel = CaptureViewModel(
            imageProcessor = processor,
            uuidGenerator = FixedUuidGenerator("draft-1"),
        )

        viewModel.onAction(CaptureAction.ImageSelected("content://capture/source"))
        viewModel.onAction(CaptureAction.RotateCropClockwise)
        viewModel.onAction(CaptureAction.ConfirmCrop)
        advanceUntilIdle()

        assertThat(processor.inputs).containsExactly(
            CaptureImageProcessInput(
                draftId = "draft-1",
                sourceUri = "content://capture/source",
                rotationDegrees = 90,
            ),
        )
        assertThat(viewModel.uiState.value.stage)
            .isEqualTo(CaptureStage.Reviewing("draft-1"))
        assertThat(viewModel.uiState.value.qualityWarning)
            .contains("最低质量")
        assertThat(viewModel.processedImage("draft-1"))
            .isEqualTo(processedImage(reachedMinimumQuality = true))
    }

    @Test
    fun `image processing failure returns to crop with retryable Chinese error`() = runTest {
        val viewModel = CaptureViewModel(
            imageProcessor = RecordingImageProcessor(
                failure = IllegalStateException("bad image"),
            ),
            uuidGenerator = FixedUuidGenerator("draft-2"),
        )

        viewModel.onAction(CaptureAction.ImageSelected("content://capture/bad"))
        viewModel.onAction(CaptureAction.ConfirmCrop)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.stage)
            .isEqualTo(CaptureStage.Crop("draft-2", "content://capture/bad"))
        assertThat(viewModel.uiState.value.errorMessage)
            .isEqualTo("图片处理失败，请重试")
    }
}

private class RecordingImageProcessor(
    private val processed: ProcessedCaptureImage = processedImage(),
    private val progressValues: List<Int> = emptyList(),
    private val failure: Throwable? = null,
) : ProcessCaptureImageUseCase {
    val inputs = mutableListOf<CaptureImageProcessInput>()

    override suspend fun invoke(
        input: CaptureImageProcessInput,
        onProgress: suspend (Int) -> Unit,
    ): ProcessedCaptureImage {
        inputs += input
        progressValues.forEach { onProgress(it) }
        failure?.let { throw it }
        return processed
    }
}

private fun processedImage(
    reachedMinimumQuality: Boolean = false,
) = ProcessedCaptureImage(
    storedImage = StoredImage(
        relativePath = "images/draft-1.jpg",
        sha256Hex = "sha256",
        width = 1440,
        height = 1080,
        thumbnailRelativePath = "thumbs/draft-1.jpg",
    ),
    imageBytes = "image".encodeToByteArray(),
    jpegQuality = if (reachedMinimumQuality) 40 else 85,
    reachedMinimumQuality = reachedMinimumQuality,
)
