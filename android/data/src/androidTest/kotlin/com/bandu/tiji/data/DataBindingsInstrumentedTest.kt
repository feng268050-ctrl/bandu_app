package com.bandu.tiji.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bandu.tiji.data.ai.ProviderAiTutorGateway
import com.bandu.tiji.data.repository.PersistentAiConfigurationRepository
import com.bandu.tiji.data.repository.PersistentProfileRepository
import com.bandu.tiji.data.repository.RoomCollectionRepository
import com.bandu.tiji.data.repository.RoomErrorItemRepository
import com.bandu.tiji.data.repository.RoomExerciseRepository
import com.bandu.tiji.data.repository.RoomStatsRepository
import com.bandu.tiji.data.repository.RoomTagRepository
import com.bandu.tiji.data.repository.RoomTutorRepository
import com.bandu.tiji.data.repository.RuntimeDeviceTransferRepository
import com.bandu.tiji.domain.repository.AiConfigurationRepository
import com.bandu.tiji.domain.repository.AiTutorGateway
import com.bandu.tiji.domain.repository.CollectionRepository
import com.bandu.tiji.domain.repository.DeviceTransferRepository
import com.bandu.tiji.domain.repository.ErrorItemRepository
import com.bandu.tiji.domain.repository.ExerciseRepository
import com.bandu.tiji.domain.repository.ProfileRepository
import com.bandu.tiji.domain.repository.StatsRepository
import com.bandu.tiji.domain.repository.TagRepository
import com.bandu.tiji.domain.repository.TutorRepository
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class DataBindingsInstrumentedTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject lateinit var collectionRepository: CollectionRepository
    @Inject lateinit var errorItemRepository: ErrorItemRepository
    @Inject lateinit var exerciseRepository: ExerciseRepository
    @Inject lateinit var tagRepository: TagRepository
    @Inject lateinit var tutorRepository: TutorRepository
    @Inject lateinit var statsRepository: StatsRepository
    @Inject lateinit var profileRepository: ProfileRepository
    @Inject lateinit var aiConfigurationRepository: AiConfigurationRepository
    @Inject lateinit var aiTutorGateway: AiTutorGateway
    @Inject lateinit var deviceTransferRepository: DeviceTransferRepository

    @Before
    fun inject() {
        hiltRule.inject()
    }

    @Test
    fun allDomainRepositoriesResolveToProductionImplementations() {
        assertThat(collectionRepository).isInstanceOf(RoomCollectionRepository::class.java)
        assertThat(errorItemRepository).isInstanceOf(RoomErrorItemRepository::class.java)
        assertThat(exerciseRepository).isInstanceOf(RoomExerciseRepository::class.java)
        assertThat(tagRepository).isInstanceOf(RoomTagRepository::class.java)
        assertThat(tutorRepository).isInstanceOf(RoomTutorRepository::class.java)
        assertThat(statsRepository).isInstanceOf(RoomStatsRepository::class.java)
        assertThat(profileRepository).isInstanceOf(PersistentProfileRepository::class.java)
        assertThat(aiConfigurationRepository)
            .isInstanceOf(PersistentAiConfigurationRepository::class.java)
        assertThat(aiTutorGateway).isInstanceOf(ProviderAiTutorGateway::class.java)
        assertThat(deviceTransferRepository)
            .isInstanceOf(RuntimeDeviceTransferRepository::class.java)
    }
}
