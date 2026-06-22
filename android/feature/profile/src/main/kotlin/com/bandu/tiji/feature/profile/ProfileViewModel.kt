package com.bandu.tiji.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bandu.tiji.core.common.result.AppError
import com.bandu.tiji.core.common.result.AppResult
import com.bandu.tiji.core.common.time.Clock
import com.bandu.tiji.core.common.time.SystemClock
import com.bandu.tiji.core.model.profile.StudentProfile
import com.bandu.tiji.core.model.enums.AiProviderType
import com.bandu.tiji.domain.repository.AiConfigurationRepository
import com.bandu.tiji.domain.repository.ProfileRepository
import com.bandu.tiji.domain.usecase.profile.UpdateStudentProfileUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val profileRepository: ProfileRepository,
    private val deviceNameStore: DeviceNameStore = InMemoryDeviceNameStore(),
    private val aiConfigurationRepository: AiConfigurationRepository =
        EmptyAiConfigurationRepository,
    clock: Clock = SystemClock(),
    private val updateStudentProfile: UpdateStudentProfileUseCase =
        UpdateStudentProfileUseCase(profileRepository, clock),
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = mutableUiState.asStateFlow()

    init {
        viewModelScope.launch {
            profileRepository.observeProfile().collect { profile ->
                mutableUiState.update {
                    it.copy(
                        studentDraft = StudentProfileDraft(
                            nickname = profile.nickname,
                            educationStage = profile.educationStage,
                            enrollmentYear = profile.enrollmentYear?.toString().orEmpty(),
                        ),
                    )
                }
            }
        }
        viewModelScope.launch {
            deviceNameStore.observeDeviceName().collect { name ->
                mutableUiState.update {
                    it.copy(
                        deviceName = name,
                        deviceNameErrorMessage = null,
                    )
                }
            }
        }
        viewModelScope.launch {
            aiConfigurationRepository.observeActiveConfiguration().collect { configuration ->
                configuration ?: return@collect
                mutableUiState.update {
                    it.copy(
                        aiDraft = AiConfigurationDraftState(
                            providerType = configuration.providerType,
                            displayName = configuration.displayName,
                            baseUrl = configuration.baseUrl,
                            apiKeyInput = "",
                            hasSavedApiKey = configuration.hasApiKey,
                        ),
                    )
                }
            }
        }
    }

    fun onAction(action: ProfileAction) {
        when (action) {
            is ProfileAction.OpenSection ->
                mutableUiState.update { it.copy(currentSection = action.section) }
            ProfileAction.Back ->
                mutableUiState.update { it.copy(currentSection = null) }
            is ProfileAction.UpdateNickname -> updateStudentDraft {
                copy(nickname = action.value)
            }
            is ProfileAction.UpdateEducationStage -> updateStudentDraft {
                copy(educationStage = action.value)
            }
            is ProfileAction.UpdateEnrollmentYear -> updateStudentDraft {
                copy(enrollmentYear = action.value.filter(Char::isDigit).take(4))
            }
            ProfileAction.SaveStudentProfile -> saveStudentProfile()
            is ProfileAction.UpdateDeviceName -> updateDeviceName(action.value)
            is ProfileAction.SelectAiProvider -> selectAiProvider(action.value)
            is ProfileAction.UpdateAiDisplayName -> updateAiDraft {
                copy(displayName = action.value)
            }
            is ProfileAction.UpdateAiBaseUrl -> updateAiDraft {
                copy(baseUrl = action.value)
            }
            is ProfileAction.UpdateAiApiKey -> updateAiDraft {
                copy(apiKeyInput = action.value)
            }
        }
    }

    private fun selectAiProvider(providerType: AiProviderType) {
        updateAiDraft {
            copy(
                providerType = providerType,
                displayName = when (providerType) {
                    AiProviderType.GEMINI -> "Gemini"
                    AiProviderType.OPENAI_COMPATIBLE -> "OpenAI-compatible"
                },
                baseUrl = when (providerType) {
                    AiProviderType.GEMINI -> GEMINI_DEFAULT_BASE_URL
                    AiProviderType.OPENAI_COMPATIBLE -> OPENAI_COMPATIBLE_DEFAULT_BASE_URL
                },
            )
        }
    }

    private fun updateAiDraft(
        transform: AiConfigurationDraftState.() -> AiConfigurationDraftState,
    ) {
        mutableUiState.update { it.copy(aiDraft = it.aiDraft.transform()) }
    }

    private fun updateDeviceName(value: String) {
        val normalized = value.take(MAX_DEVICE_NAME_LENGTH)
        mutableUiState.update {
            it.copy(
                deviceName = normalized,
                deviceNameErrorMessage = if (value.length > MAX_DEVICE_NAME_LENGTH) {
                    "设备名称最多 $MAX_DEVICE_NAME_LENGTH 个字符"
                } else {
                    null
                },
            )
        }
        viewModelScope.launch {
            runCatching { deviceNameStore.saveDeviceName(normalized) }
                .onFailure {
                    mutableUiState.update { state ->
                        state.copy(deviceNameErrorMessage = "无法保存设备名称")
                    }
                }
        }
    }

    private fun updateStudentDraft(transform: StudentProfileDraft.() -> StudentProfileDraft) {
        mutableUiState.update {
            it.copy(
                studentDraft = it.studentDraft.transform(),
                studentErrorMessage = null,
            )
        }
    }

    private fun saveStudentProfile() {
        if (mutableUiState.value.isSavingStudent) return
        val draft = mutableUiState.value.studentDraft
        val enrollmentYear = draft.enrollmentYear
            .takeIf(String::isNotBlank)
            ?.toIntOrNull()
        if (draft.enrollmentYear.isNotBlank() && enrollmentYear == null) {
            mutableUiState.update { it.copy(studentErrorMessage = "请输入有效的入学年份") }
            return
        }
        mutableUiState.update {
            it.copy(isSavingStudent = true, studentErrorMessage = null)
        }
        viewModelScope.launch {
            val result = updateStudentProfile(
                StudentProfile(
                    nickname = draft.nickname.trim(),
                    educationStage = draft.educationStage,
                    enrollmentYear = enrollmentYear,
                ),
            )
            mutableUiState.update {
                it.copy(
                    isSavingStudent = false,
                    studentErrorMessage = when (result) {
                        is AppResult.Success -> null
                        is AppResult.Failure -> result.error.toStudentMessage()
                    },
                )
            }
        }
    }

    private fun AppError.toStudentMessage(): String =
        if (this is AppError.Validation && code == "profile.enrollment_year.future") {
            "入学年份不能晚于当前年份"
        } else {
            "无法保存学生资料"
        }

    companion object {
        const val MAX_DEVICE_NAME_LENGTH = 40
    }
}
