package com.bandu.tiji.core.model.tag

import com.bandu.tiji.core.model.id.TagId

data class TagSummary(
    val id: TagId,
    val name: String,
    val subject: String,
    val isSystem: Boolean,
)

data class TagNode(
    val tag: TagSummary,
    val code: String?,
    val sortOrder: Int,
    val linkedErrorItemCount: Int,
    val children: List<TagNode>,
)
