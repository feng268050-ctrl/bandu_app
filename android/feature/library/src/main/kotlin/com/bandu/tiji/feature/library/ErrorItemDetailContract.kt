package com.bandu.tiji.feature.library

import com.bandu.tiji.core.model.erroritem.ErrorItem
import com.bandu.tiji.core.model.collection.CollectionSummary
import com.bandu.tiji.core.model.enums.MasteryLevel
import com.bandu.tiji.core.model.enums.MistakeStatus
import com.bandu.tiji.core.model.enums.PaperLevel
import com.bandu.tiji.core.model.erroritem.StoredImage
import com.bandu.tiji.core.model.id.CollectionId
import com.bandu.tiji.core.model.id.TagId
import com.bandu.tiji.core.model.navigation.NavigationIntent
import com.bandu.tiji.core.model.tag.TagSummary

data class ErrorItemDetailUiState(
    val item: ErrorItem? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val editor: ErrorItemEditDraft? = null,
    val editorErrorMessage: String? = null,
    val isSaving: Boolean = false,
    val availableCollections: List<CollectionSummary> = emptyList(),
    val availableTags: List<TagSummary> = emptyList(),
    val isUpdatingMastery: Boolean = false,
    val masteryErrorMessage: String? = null,
    val pendingDelete: ErrorItemDeleteState? = null,
)

data class ErrorItemDeleteState(
    val isDeleting: Boolean = false,
    val errorMessage: String? = null,
)

data class ErrorItemEditDraft(
    val image: StoredImage?,
    val questionText: String,
    val answerText: String,
    val analysis: String,
    val wrongAnswerText: String,
    val mistakeStatus: MistakeStatus,
    val mistakeAnalysis: String,
    val collectionId: CollectionId,
    val subject: String,
    val tagIds: Set<TagId>,
    val gradeSemester: String,
    val paperLevel: PaperLevel?,
    val notes: String,
)

sealed interface ErrorItemDetailAction {
    data object NavigateBack : ErrorItemDetailAction

    data object Retry : ErrorItemDetailAction

    data object OpenEditor : ErrorItemDetailAction

    data object DismissEditor : ErrorItemDetailAction

    data class UpdateEditor(val draft: ErrorItemEditDraft) : ErrorItemDetailAction

    data object RequestImageReplacement : ErrorItemDetailAction

    data class ReplaceImage(val image: StoredImage) : ErrorItemDetailAction

    data object SaveEditor : ErrorItemDetailAction

    data class UpdateMastery(val level: MasteryLevel) : ErrorItemDetailAction

    data object RequestDelete : ErrorItemDetailAction

    data object DismissDelete : ErrorItemDetailAction

    data object ConfirmDelete : ErrorItemDetailAction

    data object OpenTutor : ErrorItemDetailAction
}

sealed interface ErrorItemDetailEffect {
    data object NavigateBack : ErrorItemDetailEffect

    data class Navigate(
        val intent: NavigationIntent,
    ) : ErrorItemDetailEffect

    data object SelectReplacementImage : ErrorItemDetailEffect
}
