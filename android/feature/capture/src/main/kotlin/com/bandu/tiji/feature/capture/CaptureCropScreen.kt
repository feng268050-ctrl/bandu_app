package com.bandu.tiji.feature.capture

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.bandu.tiji.core.designsystem.component.BanduCard
import com.bandu.tiji.core.designsystem.component.BanduPageScaffold
import com.bandu.tiji.core.designsystem.theme.BanduSpacing

@Composable
fun CaptureCropScreen(
    stage: CaptureStage.Crop,
    onAction: (CaptureAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    BanduPageScaffold(
        title = "裁剪题目图片",
        modifier = modifier,
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(BanduSpacing.PageHorizontal),
            verticalArrangement = Arrangement.spacedBy(BanduSpacing.CardGap),
        ) {
            BanduCard(modifier = Modifier.fillMaxWidth()) {
                Text("请确认题目图片范围")
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .testTag(CROP_PREVIEW_TAG),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "图片：${stage.tempUri}\n旋转：${stage.rotationDegrees}°",
                        color = Color.White,
                    )
                }
                Text(
                    text = "当前仅支持矩形范围预览和 90° 旋转，保存前会按当前方向处理图片。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(BanduSpacing.Small),
            ) {
                OutlinedButton(
                    onClick = { onAction(CaptureAction.RotateCropClockwise) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag(CROP_ROTATE_TAG),
                ) {
                    Text("旋转 90°")
                }
                Button(
                    onClick = { onAction(CaptureAction.ConfirmCrop) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag(CROP_CONFIRM_TAG),
                ) {
                    Text("确认裁剪")
                }
            }
            OutlinedButton(
                onClick = { onAction(CaptureAction.Cancel) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(CROP_CANCEL_TAG),
            ) {
                Text("取消")
            }
        }
    }
}

internal fun CaptureStage.Crop.rotatedClockwise(): CaptureStage.Crop =
    copy(rotationDegrees = (rotationDegrees + 90) % 360)

internal const val CROP_PREVIEW_TAG = "capture-crop-preview"
internal const val CROP_ROTATE_TAG = "capture-crop-rotate"
internal const val CROP_CONFIRM_TAG = "capture-crop-confirm"
internal const val CROP_CANCEL_TAG = "capture-crop-cancel"
