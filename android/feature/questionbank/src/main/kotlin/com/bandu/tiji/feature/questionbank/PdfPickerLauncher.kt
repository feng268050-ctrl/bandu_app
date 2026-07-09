package com.bandu.tiji.feature.questionbank

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

@Composable
fun PdfPickerLauncher(
    onPicked: (String) -> Unit,
    onCancelled: () -> Unit,
) {
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) {
            onCancelled()
        } else {
            onPicked(uri.toString())
        }
    }

    LaunchedEffect(Unit) {
        launcher.launch(arrayOf("application/pdf"))
    }
}
