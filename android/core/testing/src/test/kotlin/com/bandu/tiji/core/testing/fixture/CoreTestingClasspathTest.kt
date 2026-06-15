package com.bandu.tiji.core.testing.fixture

import com.bandu.tiji.ai.api.provider.AiProvider
import com.bandu.tiji.core.common.time.Clock
import com.bandu.tiji.core.model.profile.StudentProfile
import com.bandu.tiji.domain.repository.CollectionRepository
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CoreTestingClasspathTest {
    @Test
    fun `module exposes all fixture contract dependencies`() {
        val contractTypes = setOf(
            Clock::class,
            StudentProfile::class,
            CollectionRepository::class,
            AiProvider::class,
        )

        assertThat(contractTypes).hasSize(4)
        assertThat(BanduTestFixtureDsl::class.java.isAnnotation).isTrue()
    }
}
