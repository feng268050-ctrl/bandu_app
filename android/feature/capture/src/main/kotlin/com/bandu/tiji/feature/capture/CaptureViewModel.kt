package com.bandu.tiji.feature.capture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bandu.tiji.core.common.id.RandomUuidGenerator
import com.bandu.tiji.core.common.id.UuidGenerator
import com.bandu.tiji.core.model.collection.CollectionSummary
import com.bandu.tiji.core.model.erroritem.ErrorItemDraft
import com.bandu.tiji.core.model.navigation.NavigationIntent
import com.bandu.tiji.core.model.tag.TagSummary
import com.bandu.tiji.domain.ai.AiGatewayException
import com.bandu.tiji.domain.ai.AnalyzeImageRequest
import com.bandu.tiji.domain.ai.AnalyzedQuestion
import com.bandu.tiji.domain.pending.PendingAiOperation
import com.bandu.tiji.domain.pending.PendingOperationCoordinator
import com.bandu.tiji.domain.repository.AiTutorGateway
import com.bandu.tiji.domain.repository.ErrorItemRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class CaptureViewModel(
    private val imageProcessor: ProcessCaptureImageUseCase = DeterministicCaptureImageProcessor(),
    private val aiGateway: AiTutorGateway? = null,
    private val errorItemRepository: ErrorItemRepository? = null,
    private val draftFiles: CaptureDraftFiles = NoOpCaptureDraftFiles(),
    private val pendingOperationCoordinator: PendingOperationCoordinator = PendingOperationCoordinator(),
    private val uuidGenerator: UuidGenerator = RandomUuidGenerator(),
    initialCollections: List<CollectionSummary> = emptyList(),
    initialTags: List<TagSummary> = emptyList(),
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(
        CaptureUiState(
            availableCollections = initialCollections,
            availableTags = initialTags,
        ),
    )
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
            CaptureAction.ImportPdfQuestionBank -> {
                mutableEffects.trySend(CaptureEffect.Navigate(NavigationIntent.OpenQuestionBanks))
            }
            is CaptureAction.ImageSelected -> {
                val draftId = uuidGenerator.newUuid()
                mutableUiState.value = newUiState(
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
            CaptureAction.RetryAnalysis -> retryAnalysis()
            CaptureAction.ResumePendingOperation -> resumePendingOperation()
            is CaptureAction.UpdateReviewDraft -> {
                mutableUiState.value = mutableUiState.value.copy(reviewDraft = action.draft.trimmedToTagLimit())
            }
            is CaptureAction.SelectReviewCollection -> {
                updateReviewDraft { it.copy(collectionId = action.collectionId) }
            }
            is CaptureAction.ToggleReviewTag -> {
                updateReviewDraft { draft ->
                    val selected = draft.tagIds
                    when {
                        action.tagId in selected -> draft.copy(tagIds = selected - action.tagId)
                        selected.size >= MAX_REVIEW_TAGS -> draft
                        else -> draft.copy(tagIds = selected + action.tagId)
                    }
                }
            }
            CaptureAction.SaveReview -> saveReview()
            CaptureAction.Cancel -> {
                cleanupCurrentDraft()
                processingJob?.cancel()
                mutableEffects.trySend(CaptureEffect.NavigateBack)
            }
        }
    }

    private fun processCurrentCrop() {
        val crop = mutableUiState.value.stage as? CaptureStage.Crop ?: return
        if (processingJob?.isActive == true) return
        processingJob = viewModelScope.launch {
            mutableUiState.value = newUiState(
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
                val qualityWarning = processed.minimumQualityWarning()
                if (aiGateway == null) {
                    mutableUiState.value = newUiState(
                        stage = CaptureStage.Reviewing(crop.draftId),
                        qualityWarning = qualityWarning,
                    )
                } else {
                    analyzeProcessedImage(crop.draftId, processed, qualityWarning)
                }
            }.onFailure {
                cleanupDraft(crop.draftId)
                mutableUiState.value = newUiState(
                    stage = crop,
                    errorMessage = "图片处理失败，请重试",
                )
            }
        }
    }

    private fun retryAnalysis() {
        val draftId = (mutableUiState.value.stage as? CaptureStage.Analyzing)?.draftId
            ?: (mutableUiState.value.stage as? CaptureStage.Reviewing)?.draftId
            ?: return
        val processed = processedImages[draftId] ?: return
        processingJob = viewModelScope.launch {
            analyzeProcessedImage(
                draftId = draftId,
                processed = processed,
                qualityWarning = processed.minimumQualityWarning(),
            )
        }
    }

    private fun resumePendingOperation() {
        val operation = pendingOperationCoordinator.consumeForResume()
            as? PendingAiOperation.AnalyzeCapture
            ?: return
        val processed = processedImages[operation.draftId] ?: return
        processingJob = viewModelScope.launch {
            analyzeProcessedImage(
                draftId = operation.draftId,
                processed = processed,
                qualityWarning = processed.minimumQualityWarning(),
            )
        }
    }

    private suspend fun analyzeProcessedImage(
        draftId: String,
        processed: ProcessedCaptureImage,
        qualityWarning: String?,
    ) {
        val gateway = aiGateway ?: return
        mutableUiState.value = newUiState(
            stage = CaptureStage.Analyzing(draftId),
            qualityWarning = qualityWarning,
        )
        runCatching {
            gateway.analyzeImage(
                AnalyzeImageRequest(
                    imageBytes = processed.imageBytes,
                    mimeType = processed.mimeType,
                    languageInstruction = "请使用简体中文返回题目、答案、解析和错误分析。",
                ),
            )
        }.onSuccess { analyzed ->
            if (pendingOperationCoordinator.peek() == PendingAiOperation.AnalyzeCapture(draftId)) {
                pendingOperationCoordinator.clear()
            }
            mutableUiState.value = newUiState(
                stage = CaptureStage.Reviewing(draftId),
                qualityWarning = qualityWarning,
                reviewDraft = analyzed.toReviewDraft(),
            )
        }.onFailure { throwable ->
            if (throwable == AiGatewayException.ConfigurationRequired) {
                pendingOperationCoordinator.save(PendingAiOperation.AnalyzeCapture(draftId))
                mutableEffects.trySend(CaptureEffect.Navigate(NavigationIntent.OpenAiSettings))
            }
            mutableUiState.value = newUiState(
                stage = CaptureStage.Analyzing(draftId),
                errorMessage = throwable.toAnalysisErrorMessage(),
                qualityWarning = qualityWarning,
            )
        }
    }

    private fun AnalyzedQuestion.toReviewDraft(): CaptureReviewDraft =
        CaptureReviewDraft(
            subject = subject,
            questionText = questionText,
            answerText = answerText,
            analysis = analysis,
            wrongAnswerText = wrongAnswerText,
            mistakeStatus = mistakeStatus,
            mistakeAnalysis = mistakeAnalysis,
        )

    private fun updateReviewDraft(transform: (CaptureReviewDraft) -> CaptureReviewDraft) {
        val current = mutableUiState.value.reviewDraft ?: return
        mutableUiState.value = mutableUiState.value.copy(reviewDraft = transform(current).trimmedToTagLimit())
    }

    private fun CaptureReviewDraft.trimmedToTagLimit(): CaptureReviewDraft =
        if (tagIds.size <= MAX_REVIEW_TAGS) this else copy(tagIds = tagIds.take(MAX_REVIEW_TAGS))

    private fun saveReview() {
        val stage = mutableUiState.value.stage as? CaptureStage.Reviewing ?: return
        val draft = mutableUiState.value.reviewDraft ?: return
        val repository = errorItemRepository ?: run {
            mutableUiState.value = mutableUiState.value.copy(errorMessage = "无法保存错题，请稍后重试。")
            return
        }
        val collectionId = draft.collectionId ?: run {
            mutableUiState.value = mutableUiState.value.copy(errorMessage = "请选择题集后保存。")
            return
        }
        val processed = processedImages[stage.draftId] ?: run {
            mutableUiState.value = mutableUiState.value.copy(errorMessage = "图片尚未处理完成，请重试。")
            return
        }
        if (processingJob?.isActive == true) return
        processingJob = viewModelScope.launch {
            mutableUiState.value = mutableUiState.value.copy(
                stage = CaptureStage.Saving(stage.draftId),
                errorMessage = null,
            )
            runCatching {
                repository.create(
                    ErrorItemDraft(
                        collectionId = collectionId,
                        image = processed.storedImage,
                        questionText = draft.questionText,
                        answerText = draft.answerText,
                        analysis = draft.analysis,
                        wrongAnswerText = draft.wrongAnswerText,
                        mistakeStatus = draft.mistakeStatus,
                        mistakeAnalysis = draft.mistakeAnalysis,
                        subject = draft.subject,
                        tagIds = draft.tagIds,
                        gradeSemester = draft.gradeSemester,
                        paperLevel = draft.paperLevel,
                        notes = draft.notes,
                    ),
                )
            }.onSuccess { id ->
                draftFiles.markSaved(stage.draftId)
                mutableEffects.trySend(CaptureEffect.Navigate(NavigationIntent.OpenErrorItem(id.value)))
            }.onFailure {
                mutableUiState.value = mutableUiState.value.copy(
                    stage = CaptureStage.Reviewing(stage.draftId),
                    errorMessage = "保存错题失败，请检查字段后重试。",
                )
            }
        }
    }

    private fun ProcessedCaptureImage.minimumQualityWarning(): String? =
        if (reachedMinimumQuality) {
            "图片已压缩到最低质量，仍可能影响 AI 识别，请确认题目清晰。"
        } else {
            null
        }

    private fun Throwable.toAnalysisErrorMessage(): String =
        when (this) {
            AiGatewayException.Authentication -> "AI 鉴权失败，请检查 API Key 后重试。"
            AiGatewayException.RateLimited -> "AI 请求过于频繁，请稍后重试。"
            AiGatewayException.Timeout -> "AI 分析超时，请重试。"
            AiGatewayException.NetworkUnavailable -> "网络不可用，无法分析图片，请稍后重试。"
            is AiGatewayException.InvalidResponse -> "AI 返回内容无法识别，请重试。诊断码：$diagnosticCode"
            is AiGatewayException.EndpointRejected -> "AI 服务地址被安全策略拒绝：$reason"
            AiGatewayException.ConfigurationRequired -> "请先配置 AI 服务后再分析图片。"
            else -> "AI 分析失败，请重试。"
        }

    private fun cleanupCurrentDraft() {
        currentDraftId()?.let(::cleanupDraft)
    }

    private fun cleanupDraft(draftId: String) {
        processedImages.remove(draftId)
        viewModelScope.launch {
            draftFiles.cleanupDraft(draftId)
        }
    }

    private fun currentDraftId(): String? =
        when (val stage = mutableUiState.value.stage) {
            is CaptureStage.Crop -> stage.draftId
            is CaptureStage.Processing -> stage.draftId
            is CaptureStage.Analyzing -> stage.draftId
            is CaptureStage.Reviewing -> stage.draftId
            is CaptureStage.Saving -> stage.draftId
            CaptureStage.Camera,
            CaptureStage.SelectSource,
            -> null
        }

    private fun newUiState(
        stage: CaptureStage,
        errorMessage: String? = null,
        qualityWarning: String? = null,
        reviewDraft: CaptureReviewDraft? = null,
    ): CaptureUiState =
        CaptureUiState(
            stage = stage,
            errorMessage = errorMessage,
            qualityWarning = qualityWarning,
            reviewDraft = reviewDraft,
            availableCollections = mutableUiState.value.availableCollections,
            availableTags = mutableUiState.value.availableTags,
        )

    private companion object {
        const val MAX_REVIEW_TAGS = 5
    }
}
