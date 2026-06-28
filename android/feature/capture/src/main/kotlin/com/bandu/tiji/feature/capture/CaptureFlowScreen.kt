package com.bandu.tiji.feature.capture

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.bandu.tiji.core.designsystem.component.BanduPageScaffold
import com.bandu.tiji.core.designsystem.theme.BanduSpacing

@Composable
fun CaptureFlowScreen(
    uiState: CaptureUiState,
    onAction: (CaptureAction) -> Unit,
    modifier: Modifier = Modifier,
    cameraContent: (@Composable ((String) -> Unit, () -> Unit) -> Unit)? = null,
) {
    when (val stage = uiState.stage) {
        CaptureStage.SelectSource -> CaptureSourceScreen(
            onAction = onAction,
            modifier = modifier,
        )
        CaptureStage.Camera -> {
            if (cameraContent != null) {
                cameraContent(
                    { uri -> onAction(CaptureAction.ImageSelected(uri)) },
                    { onAction(CaptureAction.Cancel) },
                )
            } else {
                CameraCaptureLauncher(
                    onCaptured = { onAction(CaptureAction.ImageSelected(it)) },
                    onChoosePhoto = { onAction(CaptureAction.ChoosePhoto) },
                    onCancel = { onAction(CaptureAction.Cancel) },
                    modifier = modifier,
                )
            }
        }
        is CaptureStage.Crop -> CaptureCropScreen(
            stage = stage,
            onAction = onAction,
            modifier = modifier,
        )
        is CaptureStage.Processing -> CaptureProgressScreen(
            title = "正在处理图片",
            message = "压缩进度 ${stage.progress}%",
            progress = stage.progress / 100f,
            modifier = modifier,
        )
        is CaptureStage.Analyzing -> CaptureAnalysisScreen(
            uiState = uiState,
            onAction = onAction,
            modifier = modifier,
        )
        is CaptureStage.Reviewing -> CaptureReviewScreen(
            uiState = uiState,
            onAction = onAction,
            modifier = modifier,
        )
        is CaptureStage.Saving -> CaptureProgressScreen(
            title = "正在保存错题",
            message = "请稍候",
            progress = null,
            modifier = modifier,
        )
    }
}

@Composable
private fun CaptureProgressScreen(
    title: String,
    message: String,
    progress: Float?,
    modifier: Modifier = Modifier,
) {
    BanduPageScaffold(title = title, modifier = modifier) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(BanduSpacing.PageHorizontal)
                .testTag(CAPTURE_PROGRESS_TAG),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(message)
            if (progress == null) {
                LinearProgressIndicator()
            } else {
                LinearProgressIndicator(progress = { progress })
            }
        }
    }
}

@Composable
private fun CaptureAnalysisScreen(
    uiState: CaptureUiState,
    onAction: (CaptureAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    BanduPageScaffold(title = "正在分析题目", modifier = modifier) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(BanduSpacing.PageHorizontal)
                .testTag(CAPTURE_ANALYSIS_TAG),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            uiState.errorMessage?.let { message ->
                Text(message)
                Button(onClick = { onAction(CaptureAction.RetryAnalysis) }) {
                    Text("重试")
                }
            } ?: run {
                Text("AI 正在分析图片")
                LinearProgressIndicator()
            }
            OutlinedButton(onClick = { onAction(CaptureAction.Cancel) }) {
                Text("取消")
            }
        }
    }
}

internal const val CAPTURE_PROGRESS_TAG = "capture-flow-progress"
internal const val CAPTURE_ANALYSIS_TAG = "capture-flow-analysis"
