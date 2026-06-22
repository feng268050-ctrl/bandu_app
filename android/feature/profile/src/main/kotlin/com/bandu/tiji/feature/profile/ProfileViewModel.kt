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
import com.bandu.tiji.domain.ai.AiConfigurationDraft
import com.bandu.tiji.domain.usecase.aiconfig.SaveAndActivateAiConfigurationUseCase
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
    private val saveAiConfiguration: SaveAndActivateAiConfigurationUseCase =
        SaveAndActivateAiConfigurationUseCase(aiConfigurationRepository),
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
                            analysisModel = configuration.analysisModel,
                            tutorModel = configuration.tutorModel,
                        ),
                        isAiConfigurationActive = true,
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
            is ProfileAction.UpdateAnalysisModel -> updateAiDraft {
                copy(analysisModel = action.value)
            }
            is ProfileAction.UpdateTutorModel -> updateAiDraft {
                copy(tutorModel = action.value)
            }
            ProfileAction.SaveAiConfiguration -> saveAiConfiguration()
            is ProfileAction.RequestPrivateHttp -> {
                if (action.enabled) {
                    mutableUiState.update {
                        it.copy(showPrivateHttpRiskConfirmation = true)
                    }
                } else {
                    updateAiDraft { copy(allowPrivateCleartext = false) }
                }
            }
            ProfileAction.ConfirmPrivateHttp -> {
                mutableUiState.update {
                    it.copy(
                        aiDraft = it.aiDraft.copy(allowPrivateCleartext = true),
                        showPrivateHttpRiskConfirmation = false,
                        isAiConfigurationActive = false,
                    )
                }
            }
            ProfileAction.DismissPrivateHttp -> {
                mutableUiState.update {
                    it.copy(showPrivateHttpRiskConfirmation = false)
                }
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
        mutableUiState.update {
            it.copy(
                aiDraft = it.aiDraft.transform(),
                aiValidationMessage = null,
                isAiConfigurationActive = false,
            )
        }
    }

    private fun saveAiConfiguration() {
        if (mutableUiState.value.isValidatingAi) return
        val draft = mutableUiState.value.aiDraft
        mutableUiState.update {
            it.copy(
                isValidatingAi = true,
                aiValidationMessage = null,
            )
        }
        viewModelScope.launch {
            val result = saveAiConfiguration(
                AiConfigurationDraft(
                    providerType = draft.providerType,
                    displayName = draft.displayName.trim(),
                    baseUrl = draft.baseUrl.trim(),
                    apiKey = draft.apiKeyInput.trim().ifBlank { null },
                    analysisModel = draft.analysisModel.trim(),
                    tutorModel = draft.tutorModel.trim(),
                    allowPrivateCleartext = draft.allowPrivateCleartext,
                ),
            )
            mutableUiState.update { current ->
                when (result) {
                    is AppResult.Success -> current.copy(
                        isValidatingAi = false,
                        aiValidationMessage = "连接验证成功，配置已激活",
                        isAiConfigurationActive = true,
                        aiDraft = current.aiDraft.copy(
                            apiKeyInput = "",
                            hasSavedApiKey = true,
                        ),
                    )
                    is AppResult.Failure -> current.copy(
                        isValidatingAi = false,
                        aiValidationMessage = "连接验证失败，请检查地址、模型和密钥",
                        isAiConfigurationActive = false,
                    )
                }
            }
        }
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
