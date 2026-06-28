package com.bandu.tiji.feature.capture

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

@Composable
fun PhotoPickerLauncher(
    onPicked: (String) -> Unit,
    onCancelled: () -> Unit,
) {
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri == null) {
            onCancelled()
        } else {
            onPicked(uri.toString())
        }
    }

    LaunchedEffect(Unit) {
        launcher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
        )
    }
}
