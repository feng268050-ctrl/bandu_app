package com.bandu.tiji.feature.capture

interface CaptureDraftFiles {
    suspend fun cleanupDraft(draftId: String)

    suspend fun markSaved(draftId: String)
}

class NoOpCaptureDraftFiles : CaptureDraftFiles {
    override suspend fun cleanupDraft(draftId: String) = Unit

    override suspend fun markSaved(draftId: String) = Unit
}
