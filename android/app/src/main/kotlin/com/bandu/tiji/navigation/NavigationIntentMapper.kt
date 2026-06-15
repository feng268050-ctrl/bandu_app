package com.bandu.tiji.navigation

import com.bandu.tiji.core.model.navigation.NavigationIntent

fun NavigationIntent.toAppDestination(): AppDestination = when (this) {
    NavigationIntent.OpenCapture -> CaptureDestination
    NavigationIntent.OpenLibrary -> LibraryDestination
    NavigationIntent.OpenTags -> TagsDestination
    NavigationIntent.OpenStats -> StatsDestination
    is NavigationIntent.OpenCollection -> CollectionDestination(collectionId)
    is NavigationIntent.OpenErrorItem -> ErrorItemDetailDestination(errorItemId)
    is NavigationIntent.OpenTutor -> TutorSessionDestination(
        sessionId = null,
        errorItemId = errorItemId,
    )
    NavigationIntent.OpenAiSettings -> AiSettingsDestination
}
