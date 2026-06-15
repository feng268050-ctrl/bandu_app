package com.bandu.tiji.domain.repository

import com.bandu.tiji.core.model.id.TagId
import com.bandu.tiji.core.model.tag.TagNode
import com.bandu.tiji.core.model.tag.TagSummary
import com.bandu.tiji.domain.tag.CreateTagInput
import kotlinx.coroutines.flow.Flow

interface TagRepository {
    fun observeTree(subject: String?): Flow<List<TagNode>>

    suspend fun findTag(id: TagId): TagSummary?

    suspend fun createCustom(input: CreateTagInput): TagId

    suspend fun renameCustom(id: TagId, name: String)

    suspend fun deleteCustom(id: TagId)
}
