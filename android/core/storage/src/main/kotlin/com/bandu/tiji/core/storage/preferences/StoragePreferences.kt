package com.bandu.tiji.core.storage.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.bandu.tiji.core.model.enums.AiProviderType
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

data class PortablePreferences(
    val nickname: String = "",
    val educationStage: String? = null,
    val enrollmentYear: Int? = null,
    val avatarBackgroundIndex: Int = 0,
    val avatarImageUri: String? = null,
    val providerType: AiProviderType? = null,
    val providerDisplayName: String = "",
    val baseUrl: String = "",
    val analysisModel: String = "",
    val tutorModel: String = "",
    val analyzeImagePrompt: String = "",
    val tutorPrompt: String = "",
    val generateExercisePrompt: String = "",
    val gradeExercisePrompt: String = "",
    val promptSchemaVersion: Int = 1,
)

data class DevicePreferences(
    val deviceId: String = "",
    val deviceDisplayName: String = "",
    val activeSlot: String = "slot_a",
    val trustedPeerRecords: Set<String> = emptySet(),
    val identityKeyAlias: String = "",
    val allowPrivateHttp: Boolean = false,
    val currentTransferSessionId: String? = null,
    val pendingCommitSlot: String? = null,
    val commitFinalizedAtEpochMillis: Long? = null,
)

class PortablePreferencesStore internal constructor(
    private val store: DataStore<Preferences>,
) {
    val data: Flow<PortablePreferences> = store.safeData().map(PortableKeys::read)

    suspend fun replace(value: PortablePreferences) {
        store.edit { preferences ->
            preferences.clear()
            PortableKeys.write(preferences, value)
        }
    }
}

class DevicePreferencesStore internal constructor(
    private val store: DataStore<Preferences>,
) {
    val data: Flow<DevicePreferences> = store.safeData().map(DeviceKeys::read)

    suspend fun replace(value: DevicePreferences) {
        store.edit { preferences ->
            preferences.clear()
            DeviceKeys.write(preferences, value)
        }
    }
}

private fun DataStore<Preferences>.safeData(): Flow<Preferences> = data.catch { throwable ->
    if (throwable is IOException) {
        emit(emptyPreferences())
    } else {
        throw throwable
    }
}

private object PortableKeys {
    private val nickname = stringPreferencesKey("student_nickname")
    private val educationStage = stringPreferencesKey("education_stage")
    private val enrollmentYear = intPreferencesKey("enrollment_year")
    private val avatarBackgroundIndex = intPreferencesKey("avatar_background_index")
    private val avatarImageUri = stringPreferencesKey("avatar_image_uri")
    private val providerType = stringPreferencesKey("provider_type")
    private val providerDisplayName = stringPreferencesKey("provider_display_name")
    private val baseUrl = stringPreferencesKey("base_url")
    private val analysisModel = stringPreferencesKey("analysis_model")
    private val tutorModel = stringPreferencesKey("tutor_model")
    private val analyzeImagePrompt = stringPreferencesKey("prompt_analyze_image")
    private val tutorPrompt = stringPreferencesKey("prompt_tutor")
    private val generateExercisePrompt = stringPreferencesKey("prompt_generate_exercise")
    private val gradeExercisePrompt = stringPreferencesKey("prompt_grade_exercise")
    private val promptSchemaVersion = intPreferencesKey("prompt_schema_version")

    fun read(preferences: Preferences): PortablePreferences = PortablePreferences(
        nickname = preferences[nickname].orEmpty(),
        educationStage = preferences[educationStage],
        enrollmentYear = preferences[enrollmentYear],
        avatarBackgroundIndex = preferences[avatarBackgroundIndex] ?: 0,
        avatarImageUri = preferences[avatarImageUri],
        providerType = preferences[providerType]?.let(AiProviderType::valueOf),
        providerDisplayName = preferences[providerDisplayName].orEmpty(),
        baseUrl = preferences[baseUrl].orEmpty(),
        analysisModel = preferences[analysisModel].orEmpty(),
        tutorModel = preferences[tutorModel].orEmpty(),
        analyzeImagePrompt = preferences[analyzeImagePrompt].orEmpty(),
        tutorPrompt = preferences[tutorPrompt].orEmpty(),
        generateExercisePrompt = preferences[generateExercisePrompt].orEmpty(),
        gradeExercisePrompt = preferences[gradeExercisePrompt].orEmpty(),
        promptSchemaVersion = preferences[promptSchemaVersion] ?: 1,
    )

    fun write(
        preferences: MutablePreferences,
        value: PortablePreferences,
    ) {
        preferences[nickname] = value.nickname
        preferences.setOrRemove(educationStage, value.educationStage)
        preferences.setOrRemove(enrollmentYear, value.enrollmentYear)
        preferences[avatarBackgroundIndex] = value.avatarBackgroundIndex
        preferences.setOrRemove(avatarImageUri, value.avatarImageUri)
        preferences.setOrRemove(providerType, value.providerType?.name)
        preferences[providerDisplayName] = value.providerDisplayName
        preferences[baseUrl] = value.baseUrl
        preferences[analysisModel] = value.analysisModel
        preferences[tutorModel] = value.tutorModel
        preferences[analyzeImagePrompt] = value.analyzeImagePrompt
        preferences[tutorPrompt] = value.tutorPrompt
        preferences[generateExercisePrompt] = value.generateExercisePrompt
        preferences[gradeExercisePrompt] = value.gradeExercisePrompt
        preferences[promptSchemaVersion] = value.promptSchemaVersion
    }
}

private object DeviceKeys {
    private val deviceId = stringPreferencesKey("device_id")
    private val deviceDisplayName = stringPreferencesKey("device_display_name")
    private val activeSlot = stringPreferencesKey("active_slot")
    private val trustedPeerRecords = stringSetPreferencesKey("trusted_peer_records")
    private val identityKeyAlias = stringPreferencesKey("identity_key_alias")
    private val allowPrivateHttp = booleanPreferencesKey("allow_private_http")
    private val currentTransferSessionId = stringPreferencesKey("current_transfer_session_id")
    private val pendingCommitSlot = stringPreferencesKey("pending_commit_slot")
    private val commitFinalizedAt = longPreferencesKey("commit_finalized_at")

    fun read(preferences: Preferences): DevicePreferences = DevicePreferences(
        deviceId = preferences[deviceId].orEmpty(),
        deviceDisplayName = preferences[deviceDisplayName].orEmpty(),
        activeSlot = preferences[activeSlot] ?: "slot_a",
        trustedPeerRecords = preferences[trustedPeerRecords].orEmpty(),
        identityKeyAlias = preferences[identityKeyAlias].orEmpty(),
        allowPrivateHttp = preferences[allowPrivateHttp] ?: false,
        currentTransferSessionId = preferences[currentTransferSessionId],
        pendingCommitSlot = preferences[pendingCommitSlot],
        commitFinalizedAtEpochMillis = preferences[commitFinalizedAt],
    )

    fun write(
        preferences: MutablePreferences,
        value: DevicePreferences,
    ) {
        preferences[deviceId] = value.deviceId
        preferences[deviceDisplayName] = value.deviceDisplayName
        preferences[activeSlot] = value.activeSlot
        preferences[trustedPeerRecords] = value.trustedPeerRecords
        preferences[identityKeyAlias] = value.identityKeyAlias
        preferences[allowPrivateHttp] = value.allowPrivateHttp
        preferences.setOrRemove(currentTransferSessionId, value.currentTransferSessionId)
        preferences.setOrRemove(pendingCommitSlot, value.pendingCommitSlot)
        preferences.setOrRemove(commitFinalizedAt, value.commitFinalizedAtEpochMillis)
    }
}

private fun <T> MutablePreferences.setOrRemove(
    key: Preferences.Key<T>,
    value: T?,
) {
    if (value == null) {
        remove(key)
    } else {
        this[key] = value
    }
}
