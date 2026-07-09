package com.bandu.tiji.data

import android.content.Context
import com.bandu.tiji.ai.api.provider.AiProviderRegistry
import com.bandu.tiji.ai.gemini.DefaultGeminiHttpEngineFactory
import com.bandu.tiji.ai.gemini.GeminiAiProvider
import com.bandu.tiji.ai.openai.DefaultOpenAiHttpEngineFactory
import com.bandu.tiji.ai.openai.OpenAiCompatibleProvider
import com.bandu.tiji.core.common.id.RandomUuidGenerator
import com.bandu.tiji.core.common.id.UuidGenerator
import com.bandu.tiji.core.common.time.Clock
import com.bandu.tiji.core.common.time.SystemClock
import com.bandu.tiji.core.network.endpoint.DefaultEndpointPolicy
import com.bandu.tiji.core.network.endpoint.EndpointPolicy
import com.bandu.tiji.core.storage.device.AndroidDeviceNameResolver
import com.bandu.tiji.core.storage.db.LearningDatabase
import com.bandu.tiji.core.storage.db.LearningDatabaseFactory
import com.bandu.tiji.core.storage.image.ImageOrphanCleanupQueue
import com.bandu.tiji.core.storage.keystore.ApiKeyStore
import com.bandu.tiji.core.storage.preferences.DevicePreferencesStore
import com.bandu.tiji.core.storage.preferences.PortablePreferencesStore
import com.bandu.tiji.core.storage.preferences.StoragePreferencesFactory
import com.bandu.tiji.data.ai.ActiveAiConfigurationResolver
import com.bandu.tiji.data.ai.AiConfigurationValidationGateway
import com.bandu.tiji.data.ai.AndroidApiKeyCipherStore
import com.bandu.tiji.data.ai.ApiKeyCipherStore
import com.bandu.tiji.data.ai.ProviderAiTutorGateway
import com.bandu.tiji.data.ai.RegistryAiConfigurationValidationGateway
import com.bandu.tiji.data.ai.StorageActiveAiConfigurationResolver
import com.bandu.tiji.data.image.FilePendingImageCommitter
import com.bandu.tiji.data.image.PendingImageCommitter
import com.bandu.tiji.data.profile.DefaultLearningDataResetCoordinator
import com.bandu.tiji.data.profile.LearningDataResetCoordinator
import com.bandu.tiji.data.profile.ResetPaths
import com.bandu.tiji.data.repository.PersistentAiConfigurationRepository
import com.bandu.tiji.data.repository.PersistentProfileRepository
import com.bandu.tiji.data.repository.RoomCollectionRepository
import com.bandu.tiji.data.repository.RoomErrorItemRepository
import com.bandu.tiji.data.repository.RoomExerciseRepository
import com.bandu.tiji.data.repository.RoomQuestionBankRepository
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
import com.bandu.tiji.domain.repository.QuestionBankRepository
import com.bandu.tiji.domain.repository.StatsRepository
import com.bandu.tiji.domain.repository.TagRepository
import com.bandu.tiji.domain.repository.TutorRepository
import com.bandu.tiji.transfer.runtime.TransferRuntime
import com.bandu.tiji.transfer.runtime.TransferRuntimeConfig
import com.bandu.tiji.transfer.runtime.discovery.AndroidNsdDiscoveryBackend
import com.bandu.tiji.transfer.runtime.discovery.NsdPeerDiscovery
import com.bandu.tiji.transfer.runtime.identity.DeviceIdentityStore
import com.bandu.tiji.transfer.runtime.transport.LanTcpTransport
import com.bandu.tiji.transfer.runtime.trust.DevicePreferencesTrustedPeerStore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first

@Module
@InstallIn(SingletonComponent::class)
abstract class DataBindingsModule {
    @Binds
    abstract fun bindCollectionRepository(
        implementation: RoomCollectionRepository,
    ): CollectionRepository

    @Binds
    abstract fun bindErrorItemRepository(
        implementation: RoomErrorItemRepository,
    ): ErrorItemRepository

    @Binds
    abstract fun bindExerciseRepository(
        implementation: RoomExerciseRepository,
    ): ExerciseRepository

    @Binds
    abstract fun bindTagRepository(
        implementation: RoomTagRepository,
    ): TagRepository

    @Binds
    abstract fun bindTutorRepository(
        implementation: RoomTutorRepository,
    ): TutorRepository

    @Binds
    abstract fun bindStatsRepository(
        implementation: RoomStatsRepository,
    ): StatsRepository

    @Binds
    abstract fun bindQuestionBankRepository(
        implementation: RoomQuestionBankRepository,
    ): QuestionBankRepository

    @Binds
    abstract fun bindProfileRepository(
        implementation: PersistentProfileRepository,
    ): ProfileRepository

    @Binds
    abstract fun bindAiConfigurationRepository(
        implementation: PersistentAiConfigurationRepository,
    ): AiConfigurationRepository

    @Binds
    abstract fun bindAiTutorGateway(
        implementation: ProviderAiTutorGateway,
    ): AiTutorGateway

    @Binds
    abstract fun bindDeviceTransferRepository(
        implementation: RuntimeDeviceTransferRepository,
    ): DeviceTransferRepository

    @Binds
    abstract fun bindApiKeyCipherStore(
        implementation: AndroidApiKeyCipherStore,
    ): ApiKeyCipherStore

    @Binds
    abstract fun bindActiveAiConfigurationResolver(
        implementation: StorageActiveAiConfigurationResolver,
    ): ActiveAiConfigurationResolver

    @Binds
    abstract fun bindAiConfigurationValidationGateway(
        implementation: RegistryAiConfigurationValidationGateway,
    ): AiConfigurationValidationGateway

    @Binds
    abstract fun bindLearningDataResetCoordinator(
        implementation: DefaultLearningDataResetCoordinator,
    ): LearningDataResetCoordinator

    companion object {
        @Provides
        @Singleton
        fun provideClock(): Clock = SystemClock()

        @Provides
        @Singleton
        fun provideUuidGenerator(): UuidGenerator = RandomUuidGenerator()

        @Provides
        @Singleton
        fun provideStorageScope(): CoroutineScope =
            CoroutineScope(SupervisorJob() + Dispatchers.IO)

        @Provides
        @Singleton
        fun providePortablePreferencesStore(
            @ApplicationContext context: Context,
            scope: CoroutineScope,
        ): PortablePreferencesStore =
            StoragePreferencesFactory.createPortable(
                slotDirectory = File(context.filesDir, ACTIVE_SLOT_PATH),
                scope = scope,
            )

        @Provides
        @Singleton
        fun provideDevicePreferencesStore(
            @ApplicationContext context: Context,
            scope: CoroutineScope,
        ): DevicePreferencesStore =
            StoragePreferencesFactory.createDevice(
                deviceDirectory = File(context.filesDir, DEVICE_PATH),
                scope = scope,
            )

        @Provides
        @Singleton
        fun provideLearningDatabase(
            @ApplicationContext context: Context,
        ): LearningDatabase =
            LearningDatabaseFactory.open(
                context = context,
                databaseFile = File(context.filesDir, "$ACTIVE_SLOT_PATH/learning.db"),
            )

        @Provides
        @Singleton
        fun provideApiKeyStore(
            @ApplicationContext context: Context,
        ): ApiKeyStore =
            ApiKeyStore(
                encryptedFile = File(context.filesDir, "$DEVICE_PATH/api_key_ciphertext.bin"),
                alias = "bandu_tiji_ai_key_v1",
            )

        @Provides
        @Singleton
        fun providePendingImageCommitter(
            @ApplicationContext context: Context,
        ): PendingImageCommitter {
            val imageDirectory = File(context.filesDir, "$ACTIVE_SLOT_PATH/images")
            return FilePendingImageCommitter(
                filesRoot = context.filesDir,
                imageRoot = imageDirectory,
                cleanupQueue = ImageOrphanCleanupQueue(imageDirectory),
            )
        }

        @Provides
        @Singleton
        fun provideResetPaths(
            @ApplicationContext context: Context,
        ): ResetPaths =
            ResetPaths(
                slotsRoot = File(context.filesDir, "slots"),
                imageRoot = File(context.filesDir, "$ACTIVE_SLOT_PATH/images"),
                tempRoot = File(context.filesDir, "temp"),
                transferRoot = File(context.filesDir, "transfer"),
            )

        @Provides
        @Singleton
        fun provideTransferRuntime(
            @ApplicationContext context: Context,
            devicePreferences: DevicePreferencesStore,
            clock: Clock,
        ): TransferRuntime {
            val identityStore = DeviceIdentityStore(devicePreferences)
            val deviceNameResolver = AndroidDeviceNameResolver(context)
            return TransferRuntime(
                config = TransferRuntimeConfig(File(context.filesDir, "transfer")),
                clock = clock,
                localIdentityProvider = {
                    val defaultDisplayName = deviceNameResolver.resolve()
                    val current = devicePreferences.data.first()
                    if (deviceNameResolver.shouldReplaceStoredName(current.deviceDisplayName)) {
                        devicePreferences.replace(
                            current.copy(deviceDisplayName = defaultDisplayName),
                        )
                    }
                    identityStore.getOrCreate(defaultDisplayName = defaultDisplayName)
                },
                discovery = NsdPeerDiscovery(AndroidNsdDiscoveryBackend(context)),
                trustedPeerStore = DevicePreferencesTrustedPeerStore(devicePreferences),
                tcpTransport = LanTcpTransport(),
            )
        }

        @Provides
        @Singleton
        fun provideEndpointPolicy(): EndpointPolicy = DefaultEndpointPolicy()

        @Provides
        @Singleton
        fun provideAiProviderRegistry(
            endpointPolicy: EndpointPolicy,
        ): AiProviderRegistry =
            AiProviderRegistry(
                listOf(
                    GeminiAiProvider(
                        httpEngineFactory = DefaultGeminiHttpEngineFactory(endpointPolicy),
                    ),
                    OpenAiCompatibleProvider(
                        httpEngineFactory = DefaultOpenAiHttpEngineFactory(endpointPolicy),
                    ),
                ),
            )

        private const val ACTIVE_SLOT_PATH = "slots/slot_a"
        private const val DEVICE_PATH = "device"
    }
}
