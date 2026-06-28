package com.bandu.tiji.feature.capture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bandu.tiji.core.common.id.RandomUuidGenerator
import com.bandu.tiji.core.common.id.UuidGenerator
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class CaptureViewModel(
    private val imageProcessor: ProcessCaptureImageUseCase = DeterministicCaptureImageProcessor(),
    private val uuidGenerator: UuidGenerator = RandomUuidGenerator(),
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(CaptureUiState())
    val uiState: StateFlow<CaptureUiState> = mutableUiState.asStateFlow()

    private val mutableEffects = Channel<CaptureEffect>(Channel.BUFFERED)
    val effects = mutableEffects.receiveAsFlow()

    private val processedImages = mutableMapOf<String, ProcessedCaptureImage>()
    private var processingJob: Job? = null

    fun processedImage(draftId: String): ProcessedCaptureImage? = processedImages[draftId]

    fun onAction(action: CaptureAction) {
        when (action) {
            CaptureAction.ChooseCamera -> {
                mutableUiState.value = mutableUiState.value.copy(
                    stage = CaptureStage.Camera,
                    errorMessage = null,
                )
            }
            CaptureAction.ChoosePhoto -> {
                mutableEffects.trySend(CaptureEffect.LaunchPhotoPicker)
            }
            is CaptureAction.ImageSelected -> {
                val draftId = uuidGenerator.newUuid()
                mutableUiState.value = CaptureUiState(
                    stage = CaptureStage.Crop(
                        draftId = draftId,
                        tempUri = action.uri,
                    ),
                )
            }
            CaptureAction.RotateCropClockwise -> {
                val stage = mutableUiState.value.stage as? CaptureStage.Crop ?: return
                mutableUiState.value = mutableUiState.value.copy(stage = stage.rotatedClockwise())
            }
            CaptureAction.ConfirmCrop -> processCurrentCrop()
            CaptureAction.Cancel -> {
                processingJob?.cancel()
                mutableEffects.trySend(CaptureEffect.NavigateBack)
            }
        }
    }

    private fun processCurrentCrop() {
        val crop = mutableUiState.value.stage as? CaptureStage.Crop ?: return
        if (processingJob?.isActive == true) return
        processingJob = viewModelScope.launch {
            mutableUiState.value = CaptureUiState(
                stage = CaptureStage.Processing(
                    draftId = crop.draftId,
                    progress = 0,
                ),
            )
            runCatching {
                imageProcessor(
                    CaptureImageProcessInput(
                        draftId = crop.draftId,
                        sourceUri = crop.tempUri,
                        rotationDegrees = crop.rotationDegrees,
                    ),
                ) { progress ->
                    mutableUiState.value = mutableUiState.value.copy(
                        stage = CaptureStage.Processing(crop.draftId, progress),
                    )
                }
            }.onSuccess { processed ->
                processedImages[crop.draftId] = processed
                mutableUiState.value = CaptureUiState(
                    stage = CaptureStage.Reviewing(crop.draftId),
                    qualityWarning = if (processed.reachedMinimumQuality) {
                        "图片已压缩到最低质量，仍可能影响 AI 识别，请确认题目清晰。"
                    } else {
                        null
                    },
                )
            }.onFailure {
                mutableUiState.value = CaptureUiState(
                    stage = crop,
                    errorMessage = "图片处理失败，请重试",
                )
            }
        }
    }
}
