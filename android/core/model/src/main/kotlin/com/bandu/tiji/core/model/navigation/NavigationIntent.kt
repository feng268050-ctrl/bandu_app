package com.bandu.tiji.core.model.navigation

sealed interface NavigationIntent {
    data object OpenCapture : NavigationIntent

    data object OpenLibrary : NavigationIntent

    data object OpenTags : NavigationIntent

    data object OpenStats : NavigationIntent

    data class OpenCollection(val collectionId: String) : NavigationIntent

    data class OpenErrorItem(val errorItemId: String) : NavigationIntent

    data class OpenTutor(val errorItemId: String?) : NavigationIntent

    data object OpenAiSettings : NavigationIntent
}
