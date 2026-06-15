package com.bandu.tiji.domain.tag

import com.bandu.tiji.core.model.id.TagId

data class CreateTagInput(
    val name: String,
    val subject: String,
    val parentId: TagId? = null,
)
