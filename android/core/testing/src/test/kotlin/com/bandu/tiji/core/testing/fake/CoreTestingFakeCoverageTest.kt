package com.bandu.tiji.core.testing.fake

import com.bandu.tiji.core.common.id.UuidGenerator
import com.bandu.tiji.core.common.time.Clock
import com.bandu.tiji.core.testing.id.FixedUuidGenerator
import com.bandu.tiji.core.testing.time.TestClock
import com.bandu.tiji.domain.repository.AiConfigurationRepository
import com.bandu.tiji.domain.repository.AiTutorGateway
import com.bandu.tiji.domain.repository.CollectionRepository
import com.bandu.tiji.domain.repository.DeviceTransferRepository
import com.bandu.tiji.domain.repository.ErrorItemRepository
import com.bandu.tiji.domain.repository.ProfileRepository
import com.bandu.tiji.domain.repository.StatsRepository
import com.bandu.tiji.domain.repository.TagRepository
import com.bandu.tiji.domain.repository.TutorRepository
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CoreTestingFakeCoverageTest {
    @Test
    fun `core testing exposes every ViewModel boundary fake`() {
        val boundaries =
            listOf<Any>(
                FakeCollectionRepository() as CollectionRepository,
                FakeErrorItemRepository() as ErrorItemRepository,
                FakeTagRepository() as TagRepository,
                FakeTutorRepository() as TutorRepository,
                FakeStatsRepository() as StatsRepository,
                FakeProfileRepository() as ProfileRepository,
                FakeAiConfigurationRepository() as AiConfigurationRepository,
                FakeAiTutorGateway() as AiTutorGateway,
                FakeDeviceTransferRepository() as DeviceTransferRepository,
                TestClock() as Clock,
                FixedUuidGenerator("fixed") as UuidGenerator,
            )

        assertThat(boundaries).hasSize(11)
    }
}
