package com.bandu.tiji.navigation

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import org.junit.Test

class TopLevelDestinationTest {
    private val json = Json

    @Test
    fun `top level destinations have the required stable order`() {
        assertThat(TopLevelDestinations.ordered).containsExactly(
            HomeDestination,
            DevicesDestination,
            CaptureDestination,
            TutorSessionsDestination,
            ProfileDestination,
        ).inOrder()
    }

    @Test
    fun `every top level destination survives polymorphic serialization`() {
        TopLevelDestinations.ordered.forEach { destination ->
            val encoded = json.encodeToString<TopLevelDestination>(destination)

            assertThat(json.decodeFromString<TopLevelDestination>(encoded))
                .isEqualTo(destination)
        }
    }
}
