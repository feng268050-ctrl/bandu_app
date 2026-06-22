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
import com.bandu.tiji.domain.ai.AiDataConsentCoordinator
import com.bandu.tiji.domain.usecase.aiconfig.SaveAndActivateAiConfigurationUseCase
import com.bandu.tiji.domain.usecase.aiconfig.ResetPromptUseCase
import com.bandu.tiji.domain.usecase.aiconfig.SavePromptUseCase
import com.bandu.tiji.domain.usecase.profile.UpdateStudentProfileUseCase
import com.bandu.tiji.domain.usecase.profile.ClearLearningDataUseCase
import com.bandu.tiji.domain.usecase.profile.FactoryResetUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

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
    private val savePrompt: SavePromptUseCase = SavePromptUseCase(aiConfigurationRepository),
    private val resetPrompt: ResetPromptUseCase = ResetPromptUseCase(aiConfigurationRepository),
    private val aiDataConsent: AiDataConsentCoordinator = AiDataConsentCoordinator(),
    private val clearLearningData: ClearLearningDataUseCase =
        ClearLearningDataUseCase(profileRepository),
    private val factoryReset: FactoryResetUseCase = FactoryResetUseCase(profileRepository),
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = mutableUiState.asStateFlow()
    private val mutableEffects = Channel<ProfileEffect>(Channel.BUFFERED)
    val effects = mutableEffects.receiveAsFlow()

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
            is ProfileAction.SelectPromptType -> {
                mutableUiState.update {
                    it.copy(
                        promptEditor = PromptEditorState(
                            type = action.type,
                            template = PromptDefaults.getValue(action.type),
                        ),
                    )
                }
            }
            is ProfileAction.UpdatePromptTemplate -> {
                mutableUiState.update {
                    it.copy(
                        promptEditor = it.promptEditor.copy(
                            template = action.value,
                            errorMessage = null,
                            statusMessage = null,
                        ),
                    )
                }
            }
            ProfileAction.SavePrompt -> savePrompt()
            ProfileAction.ResetPrompt -> resetPrompt()
            ProfileAction.RequestAiDataConsent -> {
                if (aiDataConsent.isAccepted()) {
                    mutableEffects.trySend(ProfileEffect.AiDataConsentGranted)
                } else {
                    mutableUiState.update { it.copy(showAiDataConsent = true) }
                }
            }
            ProfileAction.ConfirmAiDataConsent -> {
                aiDataConsent.accept()
                mutableUiState.update { it.copy(showAiDataConsent = false) }
                mutableEffects.trySend(ProfileEffect.AiDataConsentGranted)
            }
            ProfileAction.DismissAiDataConsent -> {
                mutableUiState.update { it.copy(showAiDataConsent = false) }
            }
            ProfileAction.RequestClearLearningData -> {
                mutableUiState.update {
                    it.copy(
                        dataManagement = DataManagementState(
                            showClearLearningConfirmation = true,
                        ),
                    )
                }
            }
            is ProfileAction.UpdateDataConfirmationText -> {
                mutableUiState.update {
                    it.copy(
                        dataManagement = it.dataManagement.copy(
                            confirmationText = action.value,
                            errorMessage = null,
                        ),
                    )
                }
            }
            ProfileAction.ConfirmClearLearningData -> confirmClearLearningData()
            ProfileAction.DismissDataConfirmation -> {
                if (!mutableUiState.value.dataManagement.isWorking) {
                    mutableUiState.update { it.copy(dataManagement = DataManagementState()) }
                }
            }
            ProfileAction.RequestFactoryReset -> {
                mutableUiState.update {
                    it.copy(
                        dataManagement = DataManagementState(
                            showFactoryResetConfirmation = true,
                        ),
                    )
                }
            }
            ProfileAction.ConfirmFactoryReset -> confirmFactoryReset()
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

    private fun savePrompt() {
        val editor = mutableUiState.value.promptEditor
        if (editor.isSaving) return
        mutableUiState.update {
            it.copy(promptEditor = editor.copy(isSaving = true, errorMessage = null))
        }
        viewModelScope.launch {
            when (savePrompt(editor.type, editor.template)) {
                is AppResult.Success -> mutableUiState.update {
                    it.copy(
                        promptEditor = it.promptEditor.copy(
                            isSaving = false,
                            statusMessage = "提示词已保存",
                        ),
                    )
                }
                is AppResult.Failure -> mutableUiState.update {
                    it.copy(
                        promptEditor = it.promptEditor.copy(
                            isSaving = false,
                            errorMessage = "提示词缺少必要占位符或包含未知占位符",
                        ),
                    )
                }
            }
        }
    }

    private fun resetPrompt() {
        val editor = mutableUiState.value.promptEditor
        if (editor.isSaving) return
        mutableUiState.update {
            it.copy(promptEditor = editor.copy(isSaving = true, errorMessage = null))
        }
        viewModelScope.launch {
            when (resetPrompt(editor.type)) {
                is AppResult.Success -> mutableUiState.update {
                    it.copy(
                        promptEditor = PromptEditorState(
                            type = editor.type,
                            template = PromptDefaults.getValue(editor.type),
                            statusMessage = "已恢复默认提示词",
                        ),
                    )
                }
                is AppResult.Failure -> mutableUiState.update {
                    it.copy(
                        promptEditor = it.promptEditor.copy(
                            isSaving = false,
                            errorMessage = "无法恢复默认提示词",
                        ),
                    )
                }
            }
        }
    }

    private fun confirmClearLearningData() {
        val state = mutableUiState.value.dataManagement
        if (state.isWorking) return
        if (state.confirmationText != CLEAR_LEARNING_CONFIRMATION_TEXT) {
            mutableUiState.update {
                it.copy(
                    dataManagement = state.copy(errorMessage = "确认文本不匹配"),
                )
            }
            return
        }
        mutableUiState.update {
            it.copy(dataManagement = state.copy(isWorking = true, errorMessage = null))
        }
        viewModelScope.launch {
            when (clearLearningData()) {
                is AppResult.Success -> mutableUiState.update {
                    it.copy(
                        dataManagement = DataManagementState(
                            statusMessage = "学习数据已清除",
                        ),
                    )
                }
                is AppResult.Failure -> mutableUiState.update {
                    it.copy(
                        dataManagement = state.copy(
                            isWorking = false,
                            errorMessage = "无法清除学习数据",
                        ),
                    )
                }
            }
        }
    }

    private fun confirmFactoryReset() {
        val state = mutableUiState.value.dataManagement
        if (state.isWorking) return
        if (state.confirmationText != FACTORY_RESET_CONFIRMATION_TEXT) {
            mutableUiState.update {
                it.copy(
                    dataManagement = state.copy(errorMessage = "确认文本不匹配"),
                )
            }
            return
        }
        mutableUiState.update {
            it.copy(dataManagement = state.copy(isWorking = true, errorMessage = null))
        }
        viewModelScope.launch {
            when (factoryReset()) {
                is AppResult.Success -> {
                    aiDataConsent.reset()
                    mutableUiState.update {
                        it.copy(
                            dataManagement = DataManagementState(
                                statusMessage = "已恢复出厂设置并生成新设备身份",
                            ),
                            isAiConfigurationActive = false,
                            aiDraft = AiConfigurationDraftState(),
                        )
                    }
                }
                is AppResult.Failure -> mutableUiState.update {
                    it.copy(
                        dataManagement = state.copy(
                            isWorking = false,
                            errorMessage = "无法恢复出厂设置",
                        ),
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
        const val CLEAR_LEARNING_CONFIRMATION_TEXT = "清除学习数据"
        const val FACTORY_RESET_CONFIRMATION_TEXT = "恢复出厂设置"
    }
}
