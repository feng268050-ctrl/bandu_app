package com.bandu.tiji.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.bandu.tiji.core.model.enums.AiProviderType
import com.bandu.tiji.core.model.profile.StudentProfile
import com.bandu.tiji.core.storage.preferences.PortablePreferences
import com.bandu.tiji.core.storage.preferences.StoragePreferencesFactory
import com.bandu.tiji.core.testing.time.TestClock
import com.bandu.tiji.data.profile.LearningDataResetCoordinator
import com.bandu.tiji.domain.usecase.profile.UpdateStudentProfileUseCase
import com.google.common.truth.Truth.assertThat
import java.io.File
import java.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class PersistentProfileRepositoryTest {
    private lateinit var root: File
    private lateinit var scope: CoroutineScope

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        root = File(context.cacheDir, "profile-${System.nanoTime()}")
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }

    @After
    fun tearDown() {
        scope.cancel()
        root.deleteRecursively()
    }

    @Test
    fun `profile updates preserve AI settings`() = runTest {
        val portable = StoragePreferencesFactory.createPortable(root, scope)
        portable.replace(
            PortablePreferences(
                providerType = AiProviderType.GEMINI,
                providerDisplayName = "Gemini",
                baseUrl = "https://example.com",
                analysisModel = "analysis",
                tutorModel = "tutor",
                tutorPrompt = "custom prompt",
            ),
        )
        val repository = PersistentProfileRepository(portable, RecordingResetCoordinator())

        repository.observeProfile().test {
            assertThat(awaitItem().nickname).isEmpty()

            repository.updateProfile(StudentProfile("小伴", "junior_high", 2025))

            assertThat(awaitItem()).isEqualTo(StudentProfile("小伴", "junior_high", 2025))
            cancelAndIgnoreRemainingEvents()
        }
        val stored = portable.data.first()
        assertThat(stored.providerType).isEqualTo(AiProviderType.GEMINI)
        assertThat(stored.tutorPrompt).isEqualTo("custom prompt")
    }

    @Test
    fun `future enrollment year is rejected without changing stored profile`() = runTest {
        val portable = StoragePreferencesFactory.createPortable(root, scope)
        val repository = PersistentProfileRepository(portable, RecordingResetCoordinator())
        val useCase = UpdateStudentProfileUseCase(
            repository = repository,
            clock = TestClock(Instant.parse("2026-06-20T00:00:00Z").toEpochMilli()),
        )

        val result = useCase(StudentProfile("小伴", "junior_high", 2027))

        assertThat(result.isSuccess).isFalse()
        assertThat(repository.observeProfile().first().enrollmentYear).isNull()
    }

    @Test
    fun `clear and factory reset delegate to coordinator`() = runTest {
        val coordinator = RecordingResetCoordinator()
        val repository = PersistentProfileRepository(
            StoragePreferencesFactory.createPortable(root, scope),
            coordinator,
        )

        repository.clearLearningData()
        repository.factoryReset()

        assertThat(coordinator.clearCalls).isEqualTo(1)
        assertThat(coordinator.factoryResetCalls).isEqualTo(1)
    }

    private class RecordingResetCoordinator : LearningDataResetCoordinator {
        var clearCalls = 0
        var factoryResetCalls = 0

        override suspend fun clearLearningData() {
            clearCalls += 1
        }

        override suspend fun factoryReset() {
            factoryResetCalls += 1
        }
    }
}
