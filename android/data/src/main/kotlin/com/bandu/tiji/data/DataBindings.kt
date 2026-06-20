package com.bandu.tiji.data

import android.content.Context
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.bandu.tiji.core.common.result.AppError
import com.bandu.tiji.core.common.result.AppResult
import com.bandu.tiji.core.common.id.RandomUuidGenerator
import com.bandu.tiji.core.common.id.UuidGenerator
import com.bandu.tiji.core.common.time.Clock
import com.bandu.tiji.core.common.time.SystemClock
import com.bandu.tiji.core.model.collection.CollectionSummary
import com.bandu.tiji.core.model.enums.AiProviderType
import com.bandu.tiji.core.model.enums.ExerciseDifficulty
import com.bandu.tiji.core.model.enums.GradeResult
import com.bandu.tiji.core.model.enums.MasteryLevel
import com.bandu.tiji.core.model.enums.MistakeStatus
import com.bandu.tiji.core.model.enums.PaperLevel
import com.bandu.tiji.core.model.erroritem.ErrorItem
import com.bandu.tiji.core.model.erroritem.ErrorItemDraft
import com.bandu.tiji.core.model.erroritem.ErrorItemPatch
import com.bandu.tiji.core.model.erroritem.ErrorItemQuery
import com.bandu.tiji.core.model.erroritem.ErrorItemSummary
import com.bandu.tiji.core.model.erroritem.StoredImage
import com.bandu.tiji.core.model.id.CollectionId
import com.bandu.tiji.core.model.id.ErrorItemId
import com.bandu.tiji.core.model.id.ExerciseId
import com.bandu.tiji.core.model.id.TagId
import com.bandu.tiji.core.model.id.TutorMessageId
import com.bandu.tiji.core.model.id.TutorSessionId
import com.bandu.tiji.core.model.profile.AiConfigurationSummary
import com.bandu.tiji.core.model.profile.DeviceSummary
import com.bandu.tiji.core.model.profile.StudentProfile
import com.bandu.tiji.core.model.stats.ExerciseStats
import com.bandu.tiji.core.model.stats.MonthlyCount
import com.bandu.tiji.core.model.stats.WrongItemStats
import com.bandu.tiji.core.model.tag.TagNode
import com.bandu.tiji.core.model.tag.TagSummary
import com.bandu.tiji.core.model.tutor.Exercise
import com.bandu.tiji.core.model.tutor.TutorMessage
import com.bandu.tiji.core.model.tutor.TutorMessageRole
import com.bandu.tiji.core.model.tutor.TutorMessageStatus
import com.bandu.tiji.core.model.tutor.TutorSession
import com.bandu.tiji.core.model.tutor.TutorSessionSummary
import com.bandu.tiji.core.storage.db.LearningDatabase
import com.bandu.tiji.core.storage.db.LearningDatabaseFactory
import com.bandu.tiji.core.storage.image.ImageOrphanCleanupQueue
import com.bandu.tiji.data.image.FilePendingImageCommitter
import com.bandu.tiji.data.image.PendingImageCommitter
import com.bandu.tiji.data.repository.RoomCollectionRepository
import com.bandu.tiji.data.repository.RoomErrorItemRepository
import com.bandu.tiji.domain.ai.AiStreamEvent
import com.bandu.tiji.domain.ai.AnalyzeImageRequest
import com.bandu.tiji.domain.ai.AnalyzedQuestion
import com.bandu.tiji.domain.ai.ExerciseGrade
import com.bandu.tiji.domain.ai.ExerciseRequest
import com.bandu.tiji.domain.ai.GeneratedExercise
import com.bandu.tiji.domain.ai.GradeExerciseRequest
import com.bandu.tiji.domain.ai.TutorRequest
import com.bandu.tiji.domain.ai.AiConfiguration
import com.bandu.tiji.domain.ai.AiConfigurationDraft
import com.bandu.tiji.domain.ai.PromptType
import com.bandu.tiji.domain.ai.ValidationResult
import com.bandu.tiji.domain.repository.AiConfigurationRepository
import com.bandu.tiji.domain.repository.AiTutorGateway
import com.bandu.tiji.domain.repository.CollectionRepository
import com.bandu.tiji.domain.repository.DeviceTransferRepository
import com.bandu.tiji.domain.repository.ErrorItemRepository
import com.bandu.tiji.domain.repository.ProfileRepository
import com.bandu.tiji.domain.repository.StatsRepository
import com.bandu.tiji.domain.repository.TagRepository
import com.bandu.tiji.domain.repository.TutorRepository
import com.bandu.tiji.domain.tag.CreateTagInput
import com.bandu.tiji.domain.transfer.DiscoveryMode
import com.bandu.tiji.domain.transfer.NearbyDevice
import com.bandu.tiji.domain.transfer.PairingCode
import com.bandu.tiji.domain.transfer.PairingResult
import com.bandu.tiji.domain.transfer.TransferFailureCode
import com.bandu.tiji.domain.transfer.TransferState
import com.bandu.tiji.domain.transfer.TransferSummary
import com.bandu.tiji.domain.transfer.TrustedDevice
import com.bandu.tiji.transfer.runtime.TransferRuntime
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

@Module
@InstallIn(SingletonComponent::class)
abstract class DataBindingsModule {
    @Binds abstract fun bindCollectionRepository(impl: RoomCollectionRepository): CollectionRepository
    @Binds abstract fun bindErrorItemRepository(impl: RoomErrorItemRepository): ErrorItemRepository
    @Binds abstract fun bindTagRepository(impl: InMemoryTagRepository): TagRepository
    @Binds abstract fun bindTutorRepository(impl: InMemoryTutorRepository): TutorRepository
    @Binds abstract fun bindStatsRepository(impl: InMemoryStatsRepository): StatsRepository
    @Binds abstract fun bindProfileRepository(impl: InMemoryProfileRepository): ProfileRepository
    @Binds abstract fun bindAiConfigurationRepository(impl: InMemoryAiConfigurationRepository): AiConfigurationRepository
    @Binds abstract fun bindAiTutorGateway(impl: InMemoryAiTutorGateway): AiTutorGateway
    @Binds abstract fun bindDeviceTransferRepository(impl: InMemoryDeviceTransferRepository): DeviceTransferRepository

    companion object {
        @Provides
        @Singleton
        fun provideTransferRuntime(): TransferRuntime = TransferRuntime()

        @Provides
        @Singleton
        fun provideClock(): Clock = SystemClock()

        @Provides
        @Singleton
        fun provideUuidGenerator(): UuidGenerator = RandomUuidGenerator()

        @Provides
        @Singleton
        fun provideLearningDatabase(
            @ApplicationContext context: Context,
        ): LearningDatabase =
            LearningDatabaseFactory.open(
                context = context,
                databaseFile = File(context.filesDir, "slots/slot_a/learning.db"),
            )

        @Provides
        @Singleton
        fun providePendingImageCommitter(
            @ApplicationContext context: Context,
        ): PendingImageCommitter {
            val slotDirectory = File(context.filesDir, "slots/slot_a")
            val imageDirectory = File(slotDirectory, "images")
            return FilePendingImageCommitter(
                filesRoot = context.filesDir,
                imageRoot = imageDirectory,
                cleanupQueue = ImageOrphanCleanupQueue(imageDirectory),
            )
        }
    }
}

@Singleton
class RepositoryStore @Inject constructor() {
    val collections = MutableStateFlow<List<CollectionSummary>>(emptyList())
    val errorItems = MutableStateFlow<Map<String, StoredErrorItem>>(emptyMap())
    val tags = MutableStateFlow<Map<String, StoredTag>>(emptyMap())
    val tutorSessions = MutableStateFlow<Map<String, StoredTutorSession>>(emptyMap())
    val profile = MutableStateFlow(StudentProfile("", null, null))
    val aiConfiguration = MutableStateFlow<AiConfiguration?>(null)
    val apiKey = MutableStateFlow<String?>(null)
    val deviceId = MutableStateFlow("device-local")
    val deviceDisplayName = MutableStateFlow("伴读题集")
    val trustedPeers = MutableStateFlow<Set<String>>(emptySet())
    val activeSlot = MutableStateFlow("slot_a")
    val currentTransferSessionId = MutableStateFlow<String?>(null)
    val pendingCommitSlot = MutableStateFlow<String?>(null)
    val commitFinalizedAtEpochMillis = MutableStateFlow<Long?>(null)
    val nextCollectionId = AtomicInteger(1)
    val nextErrorItemId = AtomicInteger(1)
    val nextTagId = AtomicInteger(1)
    val nextTutorSessionId = AtomicInteger(1)
    val nextTutorMessageId = AtomicInteger(1)
    val nextExerciseId = AtomicInteger(1)
}

data class StoredErrorItem(
    val id: String,
    val collectionId: String,
    val image: StoredImage?,
    val questionText: String,
    val answerText: String,
    val analysis: String,
    val wrongAnswerText: String,
    val mistakeStatus: MistakeStatus,
    val mistakeAnalysis: String,
    val subject: String,
    val tagIds: List<String>,
    val gradeSemester: String?,
    val paperLevel: PaperLevel?,
    val notes: String,
    val masteryLevel: MasteryLevel,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

data class StoredTag(
    val id: String,
    val name: String,
    val subject: String,
    val parentId: String?,
    val sortOrder: Int,
    val code: String?,
    val isSystem: Boolean,
)

data class StoredTutorSession(
    val id: String,
    val title: String,
    val errorItemId: String?,
    val messages: MutableList<TutorMessage> = mutableListOf(),
    val exercises: MutableList<Exercise> = mutableListOf(),
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
    val updatedAtEpochMillis: Long = System.currentTimeMillis(),
)

private fun String.asCollectionId() = CollectionId(this)
private fun String.asErrorItemId() = ErrorItemId(this)
private fun String.asTagId() = TagId(this)
private fun String.asTutorSessionId() = TutorSessionId(this)
private fun String.asTutorMessageId() = TutorMessageId(this)
private fun String.asExerciseId() = ExerciseId(this)

@Singleton
class InMemoryCollectionRepository @Inject constructor(
    private val store: RepositoryStore,
) : CollectionRepository {
    override fun observeCollections(): Flow<List<CollectionSummary>> = store.collections

    override suspend fun create(name: String): CollectionId {
        val id = "collection-${store.nextCollectionId.getAndIncrement()}"
        store.collections.value = store.collections.value + CollectionSummary(id.asCollectionId(), name, 0, now())
        return id.asCollectionId()
    }

    override suspend fun rename(id: CollectionId, name: String) {
        store.collections.value = store.collections.value.map {
            if (it.id == id) it.copy(name = name, updatedAtEpochMillis = now()) else it
        }
    }

    override suspend fun delete(id: CollectionId) {
        store.collections.value = store.collections.value.filterNot { it.id == id }
        store.errorItems.value = store.errorItems.value.filterValues { it.collectionId != id.value }
    }
}

@Singleton
class InMemoryErrorItemRepository @Inject constructor(
    private val store: RepositoryStore,
) : ErrorItemRepository {
    override fun page(query: ErrorItemQuery): PagingSource<Int, ErrorItemSummary> =
        object : PagingSource<Int, ErrorItemSummary>() {
            override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ErrorItemSummary> {
                val filtered = snapshot(query)
                val start = params.key ?: 0
                val end = minOf(start + params.loadSize, filtered.size)
                val page = filtered.subList(start, end)
                val nextKey = if (end >= filtered.size) null else end
                return LoadResult.Page(page, prevKey = if (start == 0) null else maxOf(0, start - params.loadSize), nextKey = nextKey)
            }

            override fun getRefreshKey(state: PagingState<Int, ErrorItemSummary>): Int? = null
        }

    override fun observe(id: ErrorItemId): Flow<ErrorItem?> =
        store.errorItems.map { items -> items[id.value]?.toDomain() }

    override suspend fun create(draft: ErrorItemDraft): ErrorItemId {
        val id = "error-${store.nextErrorItemId.getAndIncrement()}"
        store.errorItems.value = store.errorItems.value + (id to StoredErrorItem(
            id = id,
            collectionId = draft.collectionId.value,
            image = draft.image,
            questionText = draft.questionText,
            answerText = draft.answerText,
            analysis = draft.analysis,
            wrongAnswerText = draft.wrongAnswerText,
            mistakeStatus = draft.mistakeStatus,
            mistakeAnalysis = draft.mistakeAnalysis,
            subject = draft.subject,
            tagIds = draft.tagIds.map(TagId::value),
            gradeSemester = draft.gradeSemester,
            paperLevel = draft.paperLevel,
            notes = draft.notes,
            masteryLevel = draft.masteryLevel,
            createdAtEpochMillis = now(),
            updatedAtEpochMillis = now(),
        ))
        return id.asErrorItemId()
    }

    override suspend fun update(id: ErrorItemId, patch: ErrorItemPatch) {
        val current = store.errorItems.value[id.value] ?: return
        val updated = current.copy(
            collectionId = patch.collectionId?.value ?: current.collectionId,
            image = patch.image ?: current.image,
            questionText = patch.questionText ?: current.questionText,
            answerText = patch.answerText ?: current.answerText,
            analysis = patch.analysis ?: current.analysis,
            wrongAnswerText = patch.wrongAnswerText ?: current.wrongAnswerText,
            mistakeStatus = patch.mistakeStatus ?: current.mistakeStatus,
            mistakeAnalysis = patch.mistakeAnalysis ?: current.mistakeAnalysis,
            subject = patch.subject ?: current.subject,
            tagIds = patch.tagIds?.map(TagId::value) ?: current.tagIds,
            gradeSemester = patch.gradeSemester ?: current.gradeSemester,
            paperLevel = patch.paperLevel ?: current.paperLevel,
            notes = patch.notes ?: current.notes,
            masteryLevel = patch.masteryLevel ?: current.masteryLevel,
            updatedAtEpochMillis = now(),
        )
        store.errorItems.value = store.errorItems.value + (id.value to updated)
    }

    override suspend fun delete(ids: Set<ErrorItemId>) {
        store.errorItems.value = store.errorItems.value.filterKeys { key -> key !in ids.map(ErrorItemId::value).toSet() }
    }

    private fun snapshot(query: ErrorItemQuery): List<ErrorItemSummary> {
        val collections = store.collections.value.associateBy { it.id.value }
        val tagsByItem = store.errorItems.value.values.associate { item ->
            item.id to item.tagIds.mapNotNull { tagId -> store.tags.value[tagId]?.toSummary() }
        }
        return store.errorItems.value.values.asSequence()
            .filter { query.collectionId?.let { collectionId -> it.collectionId == collectionId.value } != false }
            .filter { query.keyword.isBlank() || it.questionText.contains(query.keyword, ignoreCase = true) || it.analysis.contains(query.keyword, ignoreCase = true) }
            .filter { query.masteryLevels.isEmpty() || query.masteryLevels.contains(it.masteryLevel) }
            .filter { query.createdAfterEpochMillis?.let { createdAfter -> it.createdAtEpochMillis >= createdAfter } != false }
            .filter { query.tagIds.isEmpty() || query.tagIds.map(TagId::value).all { tagId -> it.tagIds.contains(tagId) } }
            .filter { query.gradeSemester == null || it.gradeSemester == query.gradeSemester }
            .filter { query.paperLevels.isEmpty() || (it.paperLevel != null && query.paperLevels.contains(it.paperLevel)) }
            .sortedWith(compareByDescending<StoredErrorItem> { it.updatedAtEpochMillis }.thenBy { it.id })
            .map { item ->
                ErrorItemSummary(
                    id = item.id.asErrorItemId(),
                    collectionId = item.collectionId.asCollectionId(),
                    collectionName = collections[item.collectionId]?.name ?: "",
                    thumbnailPath = item.image?.thumbnailRelativePath,
                    questionPreview = item.questionText.take(60),
                    tags = tagsByItem[item.id].orEmpty(),
                    masteryLevel = item.masteryLevel,
                    createdAtEpochMillis = item.createdAtEpochMillis,
                )
            }
            .toList()
    }
}

@Singleton
class InMemoryTagRepository @Inject constructor(
    private val store: RepositoryStore,
) : TagRepository {
    override fun observeTree(subject: String?): Flow<List<TagNode>> =
        store.tags.map { tags ->
            tags.values.filter { subject == null || it.subject == subject }
                .sortedBy { it.sortOrder }
                .groupBy { it.parentId }
                .getOrDefault(null, emptyList())
                .map { it.toNode(children = emptyList()) }
        }

    override suspend fun findTag(id: TagId): TagSummary? = store.tags.value[id.value]?.toSummary()

    override suspend fun createCustom(input: CreateTagInput): TagId {
        val id = "tag-${store.nextTagId.getAndIncrement()}"
        store.tags.value = store.tags.value + (id to StoredTag(id, input.name, input.subject, input.parentId?.value, 0, null, false))
        return id.asTagId()
    }

    override suspend fun renameCustom(id: TagId, name: String) {
        store.tags.value = store.tags.value.mapValues { (tagId, tag) ->
            if (tagId == id.value) tag.copy(name = name) else tag
        }
    }

    override suspend fun deleteCustom(id: TagId) {
        store.tags.value = store.tags.value.filterKeys { it != id.value }
    }
}

@Singleton
class InMemoryTutorRepository @Inject constructor(
    private val store: RepositoryStore,
) : TutorRepository {
    override fun observeSessions(): Flow<List<TutorSessionSummary>> = store.tutorSessions.map { sessions ->
        sessions.values.sortedByDescending { it.updatedAtEpochMillis }.map { session ->
            TutorSessionSummary(session.id.asTutorSessionId(), session.title, session.errorItemId?.asErrorItemId(), session.updatedAtEpochMillis)
        }
    }

    override fun observeSession(id: TutorSessionId): Flow<TutorSession?> = store.tutorSessions.map { sessions ->
        sessions[id.value]?.let { session ->
            TutorSession(
                id = id,
                title = session.title,
                errorItemId = session.errorItemId?.asErrorItemId(),
                messages = session.messages,
                exercises = session.exercises,
                createdAtEpochMillis = session.createdAtEpochMillis,
                updatedAtEpochMillis = session.updatedAtEpochMillis,
            )
        }
    }

    override suspend fun getOrCreate(errorItemId: ErrorItemId?): TutorSessionId {
        val existing = store.tutorSessions.value.values.firstOrNull { it.errorItemId == errorItemId?.value }
        if (existing != null) return existing.id.asTutorSessionId()
        val id = "session-${store.nextTutorSessionId.getAndIncrement()}"
        store.tutorSessions.value = store.tutorSessions.value + (id to StoredTutorSession(id, "会话 $id", errorItemId?.value))
        return id.asTutorSessionId()
    }

    override suspend fun appendUserMessage(sessionId: TutorSessionId, text: String): TutorMessageId = appendMessage(sessionId, TutorMessageRole.USER, text, TutorMessageStatus.COMPLETE)

    override suspend fun appendAssistantMessage(sessionId: TutorSessionId, text: String): TutorMessageId = appendMessage(sessionId, TutorMessageRole.ASSISTANT, text, TutorMessageStatus.COMPLETE)

    override suspend fun deleteSession(id: TutorSessionId) {
        store.tutorSessions.value = store.tutorSessions.value.filterKeys { it != id.value }
    }

    private fun appendMessage(
        sessionId: TutorSessionId,
        role: TutorMessageRole,
        text: String,
        status: TutorMessageStatus,
    ): TutorMessageId {
        val session = store.tutorSessions.value[sessionId.value] ?: StoredTutorSession(sessionId.value, "会话 ${sessionId.value}", null)
        val messageId = "message-${store.nextTutorMessageId.getAndIncrement()}"
        val message = TutorMessage(
            id = messageId.asTutorMessageId(),
            role = role,
            content = text,
            status = status,
            sequence = session.messages.size,
            createdAtEpochMillis = now(),
        )
        session.messages += message
        store.tutorSessions.value = store.tutorSessions.value + (sessionId.value to session.copy(updatedAtEpochMillis = now()))
        return messageId.asTutorMessageId()
    }
}

@Singleton
class InMemoryStatsRepository @Inject constructor(
    private val store: RepositoryStore,
) : StatsRepository {
    override fun observeWrongItemStats(): Flow<WrongItemStats> = combine(store.errorItems, store.collections) { errorItems, collections ->
        val totalCount = errorItems.size
        val masteredCount = errorItems.values.count { it.masteryLevel == MasteryLevel.MASTERED }
        WrongItemStats(
            totalCount = totalCount,
            masteredCount = masteredCount,
            subjectCounts = errorItems.values.groupingBy { it.subject }.eachCount(),
            monthlyNewCounts = emptyList(),
        )
    }

    override fun observeExerciseStats(): Flow<ExerciseStats> = store.tutorSessions.map { sessions ->
        val exercises = sessions.values.flatMap { it.exercises }
        ExerciseStats(
            totalCount = exercises.size,
            gradedCount = exercises.count { it.finalResult != null || it.aiResult != null },
            correctCount = exercises.count { it.finalResult == GradeResult.CORRECT || it.aiResult == GradeResult.CORRECT },
            subjectCounts = exercises.groupingBy { it.subject }.eachCount(),
            difficultyCounts = exercises.groupingBy { it.difficulty }.eachCount().mapKeys { (difficulty, _) -> difficulty },
            monthlyPracticeCounts = emptyList(),
            activeDaysLastSixMonths = 0,
        )
    }
}

@Singleton
class InMemoryProfileRepository @Inject constructor(
    private val store: RepositoryStore,
) : ProfileRepository {
    override fun observeProfile(): Flow<StudentProfile> = store.profile

    override suspend fun updateProfile(profile: StudentProfile) {
        store.profile.value = profile
    }

    override suspend fun clearLearningData() {
        store.collections.value = emptyList()
        store.errorItems.value = emptyMap()
        store.tags.value = store.tags.value.filterValues { it.isSystem }
        store.tutorSessions.value = emptyMap()
    }

    override suspend fun factoryReset() {
        clearLearningData()
        store.profile.value = StudentProfile("", null, null)
        store.aiConfiguration.value = null
        store.apiKey.value = null
        store.trustedPeers.value = emptySet()
        store.activeSlot.value = "slot_a"
        store.currentTransferSessionId.value = null
        store.pendingCommitSlot.value = null
        store.commitFinalizedAtEpochMillis.value = null
    }
}

@Singleton
class InMemoryAiConfigurationRepository @Inject constructor(
    private val store: RepositoryStore,
) : AiConfigurationRepository {
    override fun observeActiveConfiguration(): Flow<AiConfiguration?> = store.aiConfiguration

    override suspend fun saveAndActivate(draft: AiConfigurationDraft): ValidationResult {
        val apiKey = draft.apiKey?.trim().orEmpty()
        if (draft.displayName.isBlank() || draft.baseUrl.isBlank() || draft.analysisModel.isBlank() || draft.tutorModel.isBlank()) {
            return ValidationResult.Failure(listOf("ai.configuration.invalid"))
        }
        store.aiConfiguration.value = AiConfiguration(
            id = "config-${draft.providerType.name.lowercase()}",
            displayName = draft.displayName,
            providerType = draft.providerType,
            baseUrl = draft.baseUrl,
            analysisModel = draft.analysisModel,
            tutorModel = draft.tutorModel,
            hasApiKey = apiKey.isNotBlank(),
        )
        store.apiKey.value = if (apiKey.isBlank()) null else apiKey
        return ValidationResult.Success
    }

    override suspend fun clearApiKey() {
        store.apiKey.value = null
        store.aiConfiguration.value = store.aiConfiguration.value?.copy(hasApiKey = false)
    }

    override suspend fun savePrompt(type: PromptType, template: String) = Unit

    override suspend fun resetPrompt(type: PromptType) = Unit
}

@Singleton
class InMemoryAiTutorGateway @Inject constructor(
    private val configurationRepository: AiConfigurationRepository,
) : AiTutorGateway {
    override suspend fun analyzeImage(request: AnalyzeImageRequest): AnalyzedQuestion {
        requireResolvedConfiguration()
        return AnalyzedQuestion(
            subject = "数学",
            knowledgePoints = listOf("代数"),
            requiresImage = false,
            wrongAnswerText = "",
            mistakeStatus = MistakeStatus.UNKNOWN,
            mistakeAnalysis = "",
            questionText = "示例题目",
            answerText = "示例答案",
            analysis = "示例解析",
        )
    }

    override fun streamTutor(request: TutorRequest): Flow<AiStreamEvent> = flow {
        requireResolvedConfiguration()
        emit(AiStreamEvent.Delta("示例回答"))
        emit(AiStreamEvent.Completed)
    }

    override suspend fun generateExercise(request: ExerciseRequest): GeneratedExercise {
        requireResolvedConfiguration()
        return GeneratedExercise("示例练习", "示例答案", "示例解析")
    }

    override suspend fun gradeExercise(request: GradeExerciseRequest): ExerciseGrade {
        requireResolvedConfiguration()
        return ExerciseGrade(
            result = GradeResult.CORRECT,
            feedback = "示例批改",
            confidence = 1.0,
        )
    }

    private suspend fun requireResolvedConfiguration(): AiConfiguration {
        val active = configurationRepository.observeActiveConfiguration().firstOrNull()
            ?: error("AI configuration not activated")
        return active
    }
}

@Singleton
class InMemoryDeviceTransferRepository @Inject constructor(
    private val runtime: TransferRuntime,
    private val store: RepositoryStore,
) : DeviceTransferRepository {
    override fun observeNearbyDevices(): Flow<List<NearbyDevice>> = runtime.observeNearbyDevices()

    override fun observeTrustedDevices(): Flow<List<TrustedDevice>> = runtime.observeTrustedDevices()

    override fun observeTransferState(): StateFlow<TransferState> = runtime.observeTransferState()

    override suspend fun startDiscovery() = runtime.startDiscovery()

    override suspend fun stopDiscovery() = runtime.stopDiscovery()

    override suspend fun createReceiveCode(): PairingCode = runtime.createReceiveCode()

    override suspend fun pair(device: NearbyDevice, code: String): PairingResult = runtime.pair(device, code)

    override suspend fun sendAll(target: TrustedDevice) = runtime.sendAll(target)

    override suspend fun acceptTransfer(sessionId: String) = runtime.acceptTransfer(sessionId)

    override suspend fun rejectTransfer(sessionId: String) = runtime.rejectTransfer(sessionId)

    override suspend fun cancelTransfer() = runtime.cancelTransfer()

    override suspend fun forgetDevice(deviceId: String) = runtime.forgetDevice(deviceId)
}

private fun StoredTag.toSummary(): TagSummary = TagSummary(id.asTagId(), name, subject, isSystem)

private fun StoredTag.toNode(children: List<TagNode>): TagNode = TagNode(
    tag = toSummary(),
    code = code,
    sortOrder = sortOrder,
    linkedErrorItemCount = 0,
    children = children,
)

private fun StoredErrorItem.toDomain(): ErrorItem = ErrorItem(
    id = id.asErrorItemId(),
    collectionId = collectionId.asCollectionId(),
    image = image,
    questionText = questionText,
    answerText = answerText,
    analysis = analysis,
    wrongAnswerText = wrongAnswerText,
    mistakeStatus = mistakeStatus,
    mistakeAnalysis = mistakeAnalysis,
    subject = subject,
    tags = emptyList(),
    gradeSemester = gradeSemester,
    paperLevel = paperLevel,
    notes = notes,
    masteryLevel = masteryLevel,
    createdAtEpochMillis = createdAtEpochMillis,
    updatedAtEpochMillis = updatedAtEpochMillis,
)

private fun now(): Long = System.currentTimeMillis()
