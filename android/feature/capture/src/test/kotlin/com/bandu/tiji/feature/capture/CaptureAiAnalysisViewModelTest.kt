package com.bandu.tiji.feature.capture

import com.bandu.tiji.core.model.enums.MistakeStatus
import com.bandu.tiji.core.testing.coroutines.MainDispatcherRule
import com.bandu.tiji.core.testing.fake.CallScript
import com.bandu.tiji.core.testing.fake.FakeAiTutorGateway
import com.bandu.tiji.core.testing.id.FixedUuidGenerator
import com.bandu.tiji.domain.ai.AiGatewayException
import com.bandu.tiji.domain.ai.AnalyzedQuestion
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CaptureAiAnalysisViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `processed image is sent to AI and fills review draft`() = runTest {
        val gateway = FakeAiTutorGateway().apply {
            enqueueAnalyze(CallScript.Return(analyzedQuestion()))
        }
        val viewModel = CaptureViewModel(
            imageProcessor = RecordingImageProcessor(processed = processedImage()),
            aiGateway = gateway,
            uuidGenerator = FixedUuidGenerator("draft-ai"),
        )

        viewModel.onAction(CaptureAction.ImageSelected("content://capture/source"))
        viewModel.onAction(CaptureAction.ConfirmCrop)
        advanceUntilIdle()

        assertThat(gateway.analyzeRequests).hasSize(1)
        assertThat(gateway.analyzeRequests.single().imageBytes)
            .isEqualTo(processedImage().imageBytes)
        assertThat(gateway.analyzeRequests.single().languageInstruction)
            .contains("简体中文")
        assertThat(viewModel.uiState.value.stage)
            .isEqualTo(CaptureStage.Reviewing("draft-ai"))
        assertThat(viewModel.uiState.value.reviewDraft).isEqualTo(
            CaptureReviewDraft(
                subject = "数学",
                questionText = "求 x",
                answerText = "x = 2",
                analysis = "移项",
                wrongAnswerText = "x = 1",
                mistakeStatus = MistakeStatus.WRONG_ATTEMPT,
                mistakeAnalysis = "移项符号错误",
            ),
        )
    }

    @Test
    fun `AI analysis error keeps draft retryable with Chinese message`() = runTest {
        val gateway = FakeAiTutorGateway().apply {
            enqueueAnalyze(CallScript.Fail(AiGatewayException.NetworkUnavailable))
            enqueueAnalyze(CallScript.Return(analyzedQuestion(questionText = "重试成功")))
        }
        val viewModel = CaptureViewModel(
            imageProcessor = RecordingImageProcessor(processed = processedImage()),
            aiGateway = gateway,
            uuidGenerator = FixedUuidGenerator("draft-retry"),
        )

        viewModel.onAction(CaptureAction.ImageSelected("content://capture/source"))
        viewModel.onAction(CaptureAction.ConfirmCrop)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.stage)
            .isEqualTo(CaptureStage.Analyzing("draft-retry"))
        assertThat(viewModel.uiState.value.errorMessage)
            .isEqualTo("网络不可用，无法分析图片，请稍后重试。")

        viewModel.onAction(CaptureAction.RetryAnalysis)
        advanceUntilIdle()

        assertThat(gateway.analyzeRequests).hasSize(2)
        assertThat(viewModel.uiState.value.stage)
            .isEqualTo(CaptureStage.Reviewing("draft-retry"))
        assertThat(viewModel.uiState.value.reviewDraft?.questionText)
            .isEqualTo("重试成功")
    }
}

private fun analyzedQuestion(
    questionText: String = "求 x",
) = AnalyzedQuestion(
    subject = "数学",
    knowledgePoints = listOf("方程"),
    requiresImage = false,
    wrongAnswerText = "x = 1",
    mistakeStatus = MistakeStatus.WRONG_ATTEMPT,
    mistakeAnalysis = "移项符号错误",
    questionText = questionText,
    answerText = "x = 2",
    analysis = "移项",
)
