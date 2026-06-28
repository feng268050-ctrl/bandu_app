package com.bandu.tiji.feature.capture

import com.bandu.tiji.core.model.navigation.NavigationIntent
import com.bandu.tiji.core.testing.coroutines.MainDispatcherRule
import com.bandu.tiji.core.testing.fake.CallScript
import com.bandu.tiji.core.testing.fake.FakeAiTutorGateway
import com.bandu.tiji.core.testing.id.FixedUuidGenerator
import com.bandu.tiji.domain.ai.AiGatewayException
import com.bandu.tiji.domain.pending.PendingAiOperation
import com.bandu.tiji.domain.pending.PendingOperationCoordinator
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CapturePendingConfigTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `missing AI configuration saves pending capture analysis and navigates to settings`() = runTest {
        val pending = PendingOperationCoordinator()
        val gateway = FakeAiTutorGateway().apply {
            enqueueAnalyze(CallScript.Fail(AiGatewayException.ConfigurationRequired))
        }
        val viewModel = CaptureViewModel(
            imageProcessor = RecordingImageProcessor(processed = processedImage()),
            aiGateway = gateway,
            pendingOperationCoordinator = pending,
            uuidGenerator = FixedUuidGenerator("draft-pending"),
        )

        viewModel.onAction(CaptureAction.ImageSelected("content://capture/source"))
        viewModel.onAction(CaptureAction.ConfirmCrop)
        advanceUntilIdle()

        assertThat(pending.peek())
            .isEqualTo(PendingAiOperation.AnalyzeCapture("draft-pending"))
        assertThat(viewModel.effects.first())
            .isEqualTo(CaptureEffect.Navigate(NavigationIntent.OpenAiSettings))
        assertThat(viewModel.uiState.value.stage)
            .isEqualTo(CaptureStage.Analyzing("draft-pending"))
        assertThat(viewModel.uiState.value.errorMessage)
            .isEqualTo("请先配置 AI 服务后再分析图片。")
    }

    @Test
    fun `validated AI configuration resumes same draft analysis only once`() = runTest {
        val pending = PendingOperationCoordinator()
        val gateway = FakeAiTutorGateway().apply {
            enqueueAnalyze(CallScript.Fail(AiGatewayException.ConfigurationRequired))
            enqueueAnalyze(CallScript.Return(analyzedQuestion(questionText = "恢复后的题目")))
        }
        val viewModel = CaptureViewModel(
            imageProcessor = RecordingImageProcessor(processed = processedImage()),
            aiGateway = gateway,
            pendingOperationCoordinator = pending,
            uuidGenerator = FixedUuidGenerator("draft-resume"),
        )

        viewModel.onAction(CaptureAction.ImageSelected("content://capture/source"))
        viewModel.onAction(CaptureAction.ConfirmCrop)
        advanceUntilIdle()

        viewModel.onAction(CaptureAction.ResumePendingOperation)
        advanceUntilIdle()
        viewModel.onAction(CaptureAction.ResumePendingOperation)
        advanceUntilIdle()

        assertThat(gateway.analyzeRequests).hasSize(2)
        assertThat(viewModel.uiState.value.stage)
            .isEqualTo(CaptureStage.Reviewing("draft-resume"))
        assertThat(viewModel.uiState.value.reviewDraft?.questionText)
            .isEqualTo("恢复后的题目")
        assertThat(pending.peek()).isNull()
    }
}
