package com.bandu.tiji.feature.tags

import com.bandu.tiji.core.model.id.TagId
import com.bandu.tiji.core.model.tag.TagNode

val StandardSubjects = listOf(
    "数学",
    "物理",
    "化学",
    "生物",
    "英语",
    "语文",
    "历史",
    "地理",
    "政治",
)

data class TagsUiState(
    val subjects: List<String> = StandardSubjects,
    val selectedSubject: String = StandardSubjects.first(),
    val tree: List<TagNode> = emptyList(),
    val expandedTagIds: Set<TagId> = emptySet(),
    val isLoading: Boolean = true,
    val loadErrorMessage: String? = null,
    val editor: TagEditorState? = null,
    val deleteConfirmation: TagDeleteConfirmation? = null,
)

data class TagEditorState(
    val mode: TagEditorMode,
    val name: String = "",
    val errorMessage: String? = null,
    val isSubmitting: Boolean = false,
)

sealed interface TagEditorMode {
    data class Create(
        val parentId: TagId?,
    ) : TagEditorMode

    data class Rename(
        val tagId: TagId,
        val originalName: String,
    ) : TagEditorMode
}

data class TagDeleteConfirmation(
    val tagId: TagId,
    val name: String,
    val linkedErrorItemCount: Int,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
)

sealed interface TagsAction {
    data class SelectSubject(val subject: String) : TagsAction

    data class ToggleExpanded(val tagId: TagId) : TagsAction

    data object Retry : TagsAction

    data class OpenCreate(val parentId: TagId?) : TagsAction

    data class OpenRename(val node: TagNode) : TagsAction

    data class OpenDelete(val node: TagNode) : TagsAction

    data class EditorNameChanged(val name: String) : TagsAction

    data object DismissEditor : TagsAction

    data object SubmitEditor : TagsAction

    data object DismissDelete : TagsAction

    data object ConfirmDelete : TagsAction

    data class OpenTag(val tagId: TagId) : TagsAction
}

sealed interface TagsEffect {
    data class OpenFilteredErrorItems(
        val tagId: TagId,
    ) : TagsEffect
}
