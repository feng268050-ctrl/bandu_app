package com.bandu.tiji.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface AppDestination

@Serializable
sealed interface TopLevelDestination : AppDestination

@Serializable
data object HomeDestination : TopLevelDestination

@Serializable
data object DevicesDestination : TopLevelDestination

@Serializable
data object CaptureDestination : TopLevelDestination

@Serializable
data object TutorSessionsDestination : TopLevelDestination

@Serializable
data object ProfileDestination : TopLevelDestination

@Serializable
data object FocusedOperationDestination : AppDestination

@Serializable
data object LibraryDestination : AppDestination

@Serializable
data object TagsDestination : AppDestination

@Serializable
data object StatsDestination : AppDestination

@Serializable
data object QuestionBanksDestination : AppDestination

@Serializable
data class CollectionDestination(
    val collectionId: String,
) : AppDestination

@Serializable
data class ErrorItemDetailDestination(
    val errorItemId: String,
) : AppDestination

@Serializable
data class TutorSessionDestination(
    val sessionId: String?,
    val errorItemId: String?,
) : AppDestination

@Serializable
data object AiSettingsDestination : AppDestination

object TopLevelDestinations {
    val ordered: List<TopLevelDestination> = listOf(
        HomeDestination,
        DevicesDestination,
        CaptureDestination,
        TutorSessionsDestination,
        ProfileDestination,
    )
}

fun shouldShowBottomNavigation(destination: AppDestination): Boolean =
    destination is TopLevelDestination
