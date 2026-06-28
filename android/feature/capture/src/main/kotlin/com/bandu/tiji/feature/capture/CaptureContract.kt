package com.bandu.tiji.feature.capture

import com.bandu.tiji.core.model.collection.CollectionSummary
import com.bandu.tiji.core.model.enums.MistakeStatus
import com.bandu.tiji.core.model.enums.PaperLevel
import com.bandu.tiji.core.model.id.CollectionId
import com.bandu.tiji.core.model.id.TagId
import com.bandu.tiji.core.model.navigation.NavigationIntent
import com.bandu.tiji.core.model.tag.TagSummary

sealed interface CaptureStage {
    data object SelectSource : CaptureStage

    data object Camera : CaptureStage

    data class Crop(
        val draftId: String,
        val tempUri: String,
        val rotationDegrees: Int = 0,
    ) : CaptureStage {
        init {
            require(draftId.isNotBlank()) { "draftId must not be blank" }
            require(tempUri.isNotBlank()) { "tempUri must not be blank" }
            require(rotationDegrees in VALID_ROTATIONS) {
                "rotationDegrees must be 0, 90, 180, or 270"
            }
        }
    }

    data class Processing(
        val draftId: String,
        val progress: Int,
    ) : CaptureStage {
        init {
            require(draftId.isNotBlank()) { "draftId must not be blank" }
            require(progress in 0..100) { "progress must be between 0 and 100" }
        }
    }

    data class Analyzing(
        val draftId: String,
    ) : CaptureStage {
        init {
            require(draftId.isNotBlank()) { "draftId must not be blank" }
        }
    }

    data class Reviewing(
        val draftId: String,
    ) : CaptureStage {
        init {
            require(draftId.isNotBlank()) { "draftId must not be blank" }
        }
    }

    data class Saving(
        val draftId: String,
    ) : CaptureStage {
        init {
            require(draftId.isNotBlank()) { "draftId must not be blank" }
        }
    }

    private companion object {
        val VALID_ROTATIONS = setOf(0, 90, 180, 270)
    }
}

data class CaptureUiState(
    val stage: CaptureStage = CaptureStage.SelectSource,
    val errorMessage: String? = null,
    val qualityWarning: String? = null,
    val reviewDraft: CaptureReviewDraft? = null,
    val availableCollections: List<CollectionSummary> = emptyList(),
    val availableTags: List<TagSummary> = emptyList(),
)

data class CaptureReviewDraft(
    val collectionId: CollectionId? = null,
    val subject: String = "",
    val questionText: String = "",
    val answerText: String = "",
    val analysis: String = "",
    val wrongAnswerText: String = "",
    val mistakeStatus: MistakeStatus = MistakeStatus.UNKNOWN,
    val mistakeAnalysis: String = "",
    val tagIds: List<TagId> = emptyList(),
    val gradeSemester: String? = null,
    val paperLevel: PaperLevel? = null,
    val notes: String = "",
) {
    val selectedTagCount: Int
        get() = tagIds.size
}

sealed interface CaptureAction {
    data object ChooseCamera : CaptureAction

    data object ChoosePhoto : CaptureAction

    data class ImageSelected(
        val uri: String,
    ) : CaptureAction

    data object RotateCropClockwise : CaptureAction

    data object ConfirmCrop : CaptureAction

    data object RetryAnalysis : CaptureAction

    data object ResumePendingOperation : CaptureAction

    data class UpdateReviewDraft(
        val draft: CaptureReviewDraft,
    ) : CaptureAction

    data class SelectReviewCollection(
        val collectionId: CollectionId,
    ) : CaptureAction

    data class ToggleReviewTag(
        val tagId: TagId,
    ) : CaptureAction

    data object Cancel : CaptureAction
}

sealed interface CaptureEffect {
    data object NavigateBack : CaptureEffect

    data object LaunchPhotoPicker : CaptureEffect

    data class Navigate(
        val intent: NavigationIntent,
    ) : CaptureEffect
}
