package com.bandu.tiji.feature.questionbank

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
fun QuestionBankRoute(
    viewModel: QuestionBankViewModel,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    var showPdfPicker by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                QuestionBankEffect.NavigateBack -> onBack()
                QuestionBankEffect.LaunchPdfPicker -> showPdfPicker = true
            }
        }
    }

    QuestionBankContent(
        uiState = uiState,
        onAction = viewModel::onAction,
    )

    if (showPdfPicker) {
        PdfPickerLauncher(
            onPicked = { uri ->
                showPdfPicker = false
                viewModel.onAction(QuestionBankAction.PdfSelected(uri))
            },
            onCancelled = {
                showPdfPicker = false
            },
        )
    }
}
