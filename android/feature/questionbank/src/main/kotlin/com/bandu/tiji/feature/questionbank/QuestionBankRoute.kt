package com.bandu.tiji.feature.questionbank

import android.content.Context
import android.os.PowerManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView

@Composable
fun QuestionBankRoute(
    viewModel: QuestionBankViewModel,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    var showPdfPicker by remember { mutableStateOf(false) }

    PdfImportKeepAwake(active = uiState.isPdfImporting)

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

@Composable
private fun PdfImportKeepAwake(active: Boolean) {
    val context = LocalContext.current.applicationContext
    val view = LocalView.current

    DisposableEffect(active, context, view) {
        if (active) {
            val previousKeepScreenOn = view.keepScreenOn
            view.keepScreenOn = true
            val wakeLock = runCatching {
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                powerManager.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "BanduTiji:PdfImport",
                ).apply {
                    setReferenceCounted(false)
                    acquire(PDF_IMPORT_WAKE_LOCK_TIMEOUT_MILLIS)
                }
            }.getOrNull()

            onDispose {
                view.keepScreenOn = previousKeepScreenOn
                if (wakeLock?.isHeld == true) {
                    wakeLock.release()
                }
            }
        } else {
            onDispose {}
        }
    }
}

private const val PDF_IMPORT_WAKE_LOCK_TIMEOUT_MILLIS = 6 * 60 * 60 * 1000L
