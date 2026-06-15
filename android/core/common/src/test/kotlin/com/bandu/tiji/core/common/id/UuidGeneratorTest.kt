package com.bandu.tiji.core.common.id

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class UuidGeneratorTest {
    @Test
    fun randomUuidGenerator_producesUniqueValidUuids() {
        val generator = RandomUuidGenerator()
        val first = generator.newUuid()
        val second = generator.newUuid()

        assertThat(first).matches(
            "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}",
        )
        assertThat(second).isNotEqualTo(first)
    }
}
