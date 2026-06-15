package com.bandu.tiji.domain.repository

import androidx.paging.PagingSource
import com.bandu.tiji.core.model.erroritem.ErrorItem
import com.bandu.tiji.core.model.erroritem.ErrorItemDraft
import com.bandu.tiji.core.model.erroritem.ErrorItemPatch
import com.bandu.tiji.core.model.erroritem.ErrorItemQuery
import com.bandu.tiji.core.model.erroritem.ErrorItemSummary
import com.bandu.tiji.core.model.id.ErrorItemId
import kotlinx.coroutines.flow.Flow

interface ErrorItemRepository {
    fun page(query: ErrorItemQuery): PagingSource<Int, ErrorItemSummary>

    fun observe(id: ErrorItemId): Flow<ErrorItem?>

    suspend fun create(draft: ErrorItemDraft): ErrorItemId

    suspend fun update(id: ErrorItemId, patch: ErrorItemPatch)

    suspend fun delete(ids: Set<ErrorItemId>)
}
