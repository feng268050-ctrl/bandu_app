package com.bandu.tiji.domain.fake

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.bandu.tiji.core.model.collection.CollectionSummary
import com.bandu.tiji.core.model.enums.ExerciseDifficulty
import com.bandu.tiji.core.model.enums.GradeResult
import com.bandu.tiji.core.model.erroritem.ErrorItem
import com.bandu.tiji.core.model.erroritem.ErrorItemDraft
import com.bandu.tiji.core.model.erroritem.ErrorItemPatch
import com.bandu.tiji.core.model.erroritem.ErrorItemQuery
import com.bandu.tiji.core.model.erroritem.ErrorItemSummary
import com.bandu.tiji.core.model.id.CollectionId
import com.bandu.tiji.core.model.id.ErrorItemId
import com.bandu.tiji.core.model.id.TagId
import com.bandu.tiji.core.model.id.TutorMessageId
import com.bandu.tiji.core.model.id.TutorSessionId
import com.bandu.tiji.core.model.profile.StudentProfile
import com.bandu.tiji.core.model.tag.TagSummary
import com.bandu.tiji.core.model.tutor.TutorSession
import com.bandu.tiji.core.model.tutor.TutorSessionSummary
import com.bandu.tiji.domain.ai.AiConfiguration
import com.bandu.tiji.domain.ai.AiConfigurationDraft
import com.bandu.tiji.domain.ai.AiStreamEvent
import com.bandu.tiji.domain.ai.AnalyzeImageRequest
import com.bandu.tiji.domain.ai.AnalyzedQuestion
import com.bandu.tiji.domain.ai.ExerciseGrade
import com.bandu.tiji.domain.ai.ExerciseRequest
import com.bandu.tiji.domain.ai.GeneratedExercise
import com.bandu.tiji.domain.ai.GradeExerciseRequest
import com.bandu.tiji.domain.ai.PromptType
import com.bandu.tiji.domain.ai.SplitQuestionBankPage
import com.bandu.tiji.domain.ai.SplitQuestionBankPageRequest
import com.bandu.tiji.domain.ai.TutorRequest
import com.bandu.tiji.domain.ai.ValidationResult
import com.bandu.tiji.domain.repository.AiConfigurationRepository
import com.bandu.tiji.domain.repository.AiTutorGateway
import com.bandu.tiji.domain.repository.CollectionRepository
import com.bandu.tiji.domain.repository.DeviceTransferRepository
import com.bandu.tiji.domain.repository.ErrorItemRepository
import com.bandu.tiji.domain.repository.ProfileRepository
import com.bandu.tiji.domain.repository.TagRepository
import com.bandu.tiji.domain.repository.TutorRepository
import com.bandu.tiji.domain.tag.CreateTagInput
import com.bandu.tiji.domain.transfer.DiscoveryMode
import com.bandu.tiji.domain.transfer.NearbyDevice
import com.bandu.tiji.domain.transfer.PairingCode
import com.bandu.tiji.domain.transfer.PairingResult
import com.bandu.tiji.domain.transfer.TransferState
import com.bandu.tiji.domain.transfer.TrustedDevice
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf

class FakeCollectionRepository : CollectionRepository {
    val collections = mutableListOf<CollectionSummary>()
    val createdNames = mutableListOf<String>()
    val deletedIds = mutableListOf<CollectionId>()
    val renamed = mutableListOf<Pair<CollectionId, String>>()

    override fun observeCollections(): Flow<List<CollectionSummary>> = flowOf(collections.toList())

    override suspend fun create(name: String): CollectionId {
        createdNames += name
        val id = CollectionId("collection-${collections.size + 1}")
        collections += CollectionSummary(id, name, 0, 0L)
        return id
    }

    override suspend fun rename(id: CollectionId, name: String) {
        renamed += id to name
    }

    override suspend fun delete(id: CollectionId) {
        deletedIds += id
        collections.removeAll { it.id == id }
    }
}

open class FakeErrorItemRepository : ErrorItemRepository {
    val createdDrafts = mutableListOf<ErrorItemDraft>()
    val updated = mutableListOf<Pair<ErrorItemId, ErrorItemPatch>>()
    val deletedIds = mutableListOf<Set<ErrorItemId>>()
    private var nextId = 1

    override fun page(query: ErrorItemQuery): PagingSource<Int, ErrorItemSummary> =
        object : PagingSource<Int, ErrorItemSummary>() {
            override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ErrorItemSummary> =
                LoadResult.Page(emptyList(), prevKey = null, nextKey = null)

            override fun getRefreshKey(state: PagingState<Int, ErrorItemSummary>): Int? = null
        }

    override fun observe(id: ErrorItemId): Flow<ErrorItem?> = flowOf(null)

    override suspend fun create(draft: ErrorItemDraft): ErrorItemId {
        createdDrafts += draft
        return ErrorItemId("error-${nextId++}")
    }

    override suspend fun update(id: ErrorItemId, patch: ErrorItemPatch) {
        updated += id to patch
    }

    override suspend fun delete(ids: Set<ErrorItemId>) {
        deletedIds += ids
    }
}

class FakeTagRepository : TagRepository {
    val tags = mutableMapOf<TagId, TagSummary>()
    val created = mutableListOf<CreateTagInput>()
    val renamed = mutableListOf<Pair<TagId, String>>()
    val deleted = mutableListOf<TagId>()
    var findFailure: Throwable? = null
    private var nextId = 1

    override fun observeTree(subject: String?) = flowOf(emptyList<com.bandu.tiji.core.model.tag.TagNode>())

    override suspend fun findTag(id: TagId): TagSummary? {
        findFailure?.let { throw it }
        return tags[id]
    }

    override suspend fun createCustom(input: CreateTagInput): TagId {
        created += input
        val id = TagId("tag-${nextId++}")
        tags[id] = TagSummary(id, input.name, input.subject, isSystem = false)
        return id
    }

    override suspend fun renameCustom(id: TagId, name: String) {
        renamed += id to name
        tags[id]?.let { existing ->
            tags[id] = existing.copy(name = name)
        }
    }

    override suspend fun deleteCustom(id: TagId) {
        deleted += id
        tags.remove(id)
    }

    fun seedSystemTag(id: TagId, name: String, subject: String) {
        tags[id] = TagSummary(id, name, subject, isSystem = true)
    }
}

class FakeTutorRepository : TutorRepository {
    val userMessages = mutableListOf<Pair<TutorSessionId, String>>()
    val assistantMessages = mutableListOf<Pair<TutorSessionId, String>>()
    val deletedSessions = mutableListOf<TutorSessionId>()

    override fun observeSessions(): Flow<List<TutorSessionSummary>> = flowOf(emptyList())

    override fun observeSession(id: TutorSessionId): Flow<TutorSession?> = flowOf(null)

    override suspend fun getOrCreate(errorItemId: ErrorItemId?): TutorSessionId =
        TutorSessionId("session-1")

    override suspend fun appendUserMessage(sessionId: TutorSessionId, text: String): TutorMessageId {
        userMessages += sessionId to text
        return TutorMessageId("msg-user-${userMessages.size}")
    }

    override suspend fun appendAssistantMessage(sessionId: TutorSessionId, text: String): TutorMessageId {
        assistantMessages += sessionId to text
        return TutorMessageId("msg-assistant-${assistantMessages.size}")
    }

    override suspend fun deleteSession(id: TutorSessionId) {
        deletedSessions += id
    }
}

class FakeProfileRepository : ProfileRepository {
    var profile = StudentProfile(nickname = "", educationStage = null, enrollmentYear = null)
    var learningDataCleared = false
    var factoryResetCalled = false

    override fun observeProfile(): Flow<StudentProfile> = flowOf(profile)

    override suspend fun updateProfile(profile: StudentProfile) {
        this.profile = profile
    }

    override suspend fun clearLearningData() {
        learningDataCleared = true
    }

    override suspend fun factoryReset() {
        factoryResetCalled = true
    }
}

class FakeAiConfigurationRepository : AiConfigurationRepository {
    var active: AiConfiguration? = null
    var savedDraft: AiConfigurationDraft? = null
    var validationResult: ValidationResult = ValidationResult.Success
    val savedPrompts = mutableMapOf<PromptType, String>()
    val resetPrompts = mutableListOf<PromptType>()
    var apiKeyCleared = false

    override fun observeActiveConfiguration(): Flow<AiConfiguration?> = flowOf(active)

    override suspend fun saveAndActivate(draft: AiConfigurationDraft): ValidationResult {
        savedDraft = draft
        return validationResult
    }

    override suspend fun clearApiKey() {
        apiKeyCleared = true
    }

    override suspend fun savePrompt(type: PromptType, template: String) {
        savedPrompts[type] = template
    }

    override suspend fun resetPrompt(type: PromptType) {
        resetPrompts += type
    }
}

class FakeAiTutorGateway : AiTutorGateway {
    val tutorRequests = mutableListOf<TutorRequest>()
    val exerciseRequests = mutableListOf<ExerciseRequest>()
    val gradeRequests = mutableListOf<GradeExerciseRequest>()
    val splitPageRequests = mutableListOf<SplitQuestionBankPageRequest>()
    var streamEvents: List<AiStreamEvent> = listOf(
        AiStreamEvent.Delta("hello"),
        AiStreamEvent.Completed,
    )
    var generatedExercise = GeneratedExercise("q", "a", "analysis")
    var exerciseGrade = ExerciseGrade(GradeResult.CORRECT, "good", 0.9)
    var splitQuestionBankPage = SplitQuestionBankPage(emptyList())

    override suspend fun analyzeImage(request: AnalyzeImageRequest): AnalyzedQuestion =
        throw UnsupportedOperationException()

    override fun streamTutor(request: TutorRequest): Flow<AiStreamEvent> {
        tutorRequests += request
        return flow { streamEvents.forEach { emit(it) } }
    }

    override suspend fun generateExercise(request: ExerciseRequest): GeneratedExercise {
        exerciseRequests += request
        return generatedExercise
    }

    override suspend fun gradeExercise(request: GradeExerciseRequest): ExerciseGrade {
        gradeRequests += request
        return exerciseGrade
    }

    override suspend fun splitQuestionBankPage(
        request: SplitQuestionBankPageRequest,
    ): SplitQuestionBankPage {
        splitPageRequests += request
        return splitQuestionBankPage
    }
}

class FakeDeviceTransferRepository : DeviceTransferRepository {
    private val transferState = MutableStateFlow<TransferState>(TransferState.Idle)
    val trustedDevices = mutableListOf<TrustedDevice>()
    var discoveryStarted = false
    var discoveryStopped = false
    var pairingCode: PairingCode = PairingCode("123456", 9_999_999L)
    var pairingResult: PairingResult = PairingResult.Success
    var sendTargets = mutableListOf<TrustedDevice>()
    var acceptedSessions = mutableListOf<String>()
    var rejectedSessions = mutableListOf<String>()
    var cancelled = false
    var forgottenDeviceIds = mutableListOf<String>()

    override fun observeNearbyDevices(): Flow<List<NearbyDevice>> = flowOf(
        listOf(NearbyDevice("peer-1", "Phone B", DiscoveryMode.PAIR)),
    )

    override fun observeTrustedDevices(): Flow<List<TrustedDevice>> = flowOf(trustedDevices.toList())

    override fun observeTransferState(): StateFlow<TransferState> = transferState.asStateFlow()

    fun setTransferState(state: TransferState) {
        transferState.value = state
    }

    override suspend fun startDiscovery() {
        discoveryStarted = true
        transferState.value = TransferState.Discovering
    }

    override suspend fun stopDiscovery() {
        discoveryStopped = true
        transferState.value = TransferState.Idle
    }

    override suspend fun createReceiveCode(): PairingCode = pairingCode

    override suspend fun pair(device: NearbyDevice, code: String): PairingResult = pairingResult

    override suspend fun sendAll(target: TrustedDevice) {
        sendTargets += target
    }

    override suspend fun acceptTransfer(sessionId: String) {
        acceptedSessions += sessionId
    }

    override suspend fun rejectTransfer(sessionId: String) {
        rejectedSessions += sessionId
    }

    override suspend fun cancelTransfer() {
        cancelled = true
    }

    override suspend fun forgetDevice(deviceId: String) {
        forgottenDeviceIds += deviceId
    }
}
