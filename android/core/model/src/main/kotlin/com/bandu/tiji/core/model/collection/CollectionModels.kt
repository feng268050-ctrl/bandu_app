package com.bandu.tiji.core.model.collection

import com.bandu.tiji.core.model.id.CollectionId

data class CollectionSummary(
    val id: CollectionId,
    val name: String,
    val errorItemCount: Int,
    val updatedAtEpochMillis: Long,
)
