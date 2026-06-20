package com.bandu.tiji.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.bandu.tiji.core.model.enums.AiProviderType
import com.bandu.tiji.core.storage.preferences.StoragePreferencesFactory
import com.bandu.tiji.core.storage.preferences.DevicePreferencesStore
import com.bandu.tiji.core.storage.preferences.PortablePreferencesStore
import com.bandu.tiji.data.ai.AiConfigurationValidationGateway
import com.bandu.tiji.data.ai.ApiKeyCipherStore
import com.bandu.tiji.domain.ai.AiConfigurationDraft
import com.bandu.tiji.domain.ai.PromptType
import com.bandu.tiji.domain.ai.ValidationResult
import com.google.common.truth.Truth.assertThat
import java.io.File
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
class PersistentAiConfigurationRepositoryTest {
    private lateinit var root: File
    private lateinit var scope: CoroutineScope
    private lateinit var keyStore: FakeApiKeyCipherStore
    private lateinit var portable: PortablePreferencesStore
    private lateinit var device: DevicePreferencesStore

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        root = File(context.cacheDir, "ai-config-${System.nanoTime()}")
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        keyStore = FakeApiKeyCipherStore()
        portable = StoragePreferencesFactory.createPortable(File(root, "slot"), scope)
        device = StoragePreferencesFactory.createDevice(File(root, "device"), scope)
    }

    @After
    fun tearDown() {
        scope.cancel()
        root.deleteRecursively()
    }

    @Test
    fun `validation failure does not activate configuration or save key`() = runTest {
        val repository = repository(
            validation = ValidationResult.Failure(listOf("authentication")),
        )

        repository.observeActiveConfiguration().test {
            assertThat(awaitItem()).isNull()

            val result = repository.saveAndActivate(draft())

            assertThat(result).isInstanceOf(ValidationResult.Failure::class.java)
            assertThat(keyStore.savedKeys).isEmpty()
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `successful validation persists configuration key and private http choice`() = runTest {
        val repository = repository(ValidationResult.Success)
        val draft = draft().copy(allowPrivateCleartext = true)

        repository.observeActiveConfiguration().test {
            assertThat(awaitItem()).isNull()

            assertThat(repository.saveAndActivate(draft)).isEqualTo(ValidationResult.Success)

            var active = checkNotNull(awaitItem())
            while (!active.hasApiKey) {
                active = checkNotNull(awaitItem())
            }
            assertThat(active.providerType).isEqualTo(AiProviderType.OPENAI_COMPATIBLE)
            assertThat(active.baseUrl).isEqualTo("https://apibest.ai/v1")
            assertThat(active.hasApiKey).isTrue()
            assertThat(keyStore.savedKeys).containsExactly("secret")
            assertThat(device.data.first().allowPrivateHttp).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `clear key and prompt edits update persisted state`() = runTest {
        val repository = repository(ValidationResult.Success)
        repository.saveAndActivate(draft())

        repository.observeActiveConfiguration().test {
            assertThat(awaitItem()?.hasApiKey).isTrue()
            repository.clearApiKey()
            assertThat(awaitItem()?.hasApiKey).isFalse()
            cancelAndIgnoreRemainingEvents()
        }

        repository.savePrompt(PromptType.TUTOR, "custom")
        assertThat(portable.data.first().tutorPrompt).isEqualTo("custom")
        repository.resetPrompt(PromptType.TUTOR)
        assertThat(portable.data.first().tutorPrompt).isEmpty()
        assertThat(keyStore.clearCalls).isEqualTo(1)
    }

    private fun repository(validation: ValidationResult): PersistentAiConfigurationRepository {
        return PersistentAiConfigurationRepository(
            portablePreferences = portable,
            devicePreferences = device,
            apiKeyStore = keyStore,
            validationGateway = AiConfigurationValidationGateway { _, _ -> validation },
        )
    }

    private fun draft(): AiConfigurationDraft =
        AiConfigurationDraft(
            providerType = AiProviderType.OPENAI_COMPATIBLE,
            displayName = "API Best",
            baseUrl = "https://apibest.ai/v1",
            apiKey = "secret",
            analysisModel = "vision-model",
            tutorModel = "chat-model",
            allowPrivateCleartext = false,
        )

    private class FakeApiKeyCipherStore : ApiKeyCipherStore {
        private var key: CharArray? = null
        val savedKeys = mutableListOf<String>()
        var clearCalls: Int = 0

        override fun read(): CharArray? = key?.copyOf()

        override fun save(apiKey: CharArray) {
            savedKeys += apiKey.concatToString()
            key = apiKey.copyOf()
        }

        override fun clear() {
            clearCalls += 1
            key?.fill('\u0000')
            key = null
        }
    }
}
