package com.bandu.tiji.navigation

import com.bandu.tiji.core.model.navigation.NavigationIntent
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class NavigationIntentMapperTest {
    @Test
    fun `every navigation intent maps to its single app-owned destination`() {
        val mappings = mapOf(
            NavigationIntent.OpenCapture to CaptureDestination,
            NavigationIntent.OpenLibrary to LibraryDestination,
            NavigationIntent.OpenTags to TagsDestination,
            NavigationIntent.OpenStats to StatsDestination,
            NavigationIntent.OpenCollection("collection-1") to
                CollectionDestination("collection-1"),
            NavigationIntent.OpenErrorItem("error-1") to
                ErrorItemDetailDestination("error-1"),
            NavigationIntent.OpenTutor("error-2") to
                TutorSessionDestination(sessionId = null, errorItemId = "error-2"),
            NavigationIntent.OpenAiSettings to AiSettingsDestination,
        )

        mappings.forEach { (intent, expectedDestination) ->
            assertThat(intent.toAppDestination()).isEqualTo(expectedDestination)
        }
        assertThat(mappings.values.map { it::class }).containsNoDuplicates()
    }

    @Test
    fun `unbound tutor intent opens a new general session`() {
        assertThat(NavigationIntent.OpenTutor(null).toAppDestination()).isEqualTo(
            TutorSessionDestination(sessionId = null, errorItemId = null),
        )
    }
}
