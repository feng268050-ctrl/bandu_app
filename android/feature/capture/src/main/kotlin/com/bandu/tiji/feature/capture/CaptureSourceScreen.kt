package com.bandu.tiji.feature.capture

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.bandu.tiji.core.designsystem.component.BanduCard
import com.bandu.tiji.core.designsystem.component.BanduPageScaffold
import com.bandu.tiji.core.designsystem.theme.BanduSpacing

@Composable
fun CaptureSourceScreen(
    onAction: (CaptureAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    BanduPageScaffold(
        title = "新增错题",
        modifier = modifier,
        navigation = {
            OutlinedButton(onClick = { onAction(CaptureAction.Cancel) }) {
                Text("取消")
            }
        },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(BanduSpacing.PageHorizontal),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            BanduCard(modifier = Modifier.fillMaxWidth()) {
                Text("选择题目图片来源")
                Button(
                    onClick = { onAction(CaptureAction.ChooseCamera) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(CAMERA_SOURCE_TAG),
                ) {
                    Text("拍照")
                }
                OutlinedButton(
                    onClick = { onAction(CaptureAction.ChoosePhoto) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(PHOTO_SOURCE_TAG),
                ) {
                    Text("从相册选择")
                }
            }
        }
    }
}

internal const val CAMERA_SOURCE_TAG = "capture-source-camera"
internal const val PHOTO_SOURCE_TAG = "capture-source-photo"
