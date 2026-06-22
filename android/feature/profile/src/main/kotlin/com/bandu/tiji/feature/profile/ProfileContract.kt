package com.bandu.tiji.feature.profile

import com.bandu.tiji.core.model.enums.AiProviderType
import com.bandu.tiji.domain.ai.PromptType

enum class ProfileSection(
    val title: String,
    val description: String,
) {
    STUDENT("学生资料", "昵称、教育阶段和入学年份"),
    AI("AI 配置", "服务、模型、密钥和提示词"),
    DEVICE("设备名称", "附近设备和配对时显示的名称"),
    DATA("数据管理", "清除学习数据或恢复出厂设置"),
    ABOUT("关于", "版本、隐私、许可和图标来源"),
}

data class ProfileUiState(
    val currentSection: ProfileSection? = null,
    val studentDraft: StudentProfileDraft = StudentProfileDraft(),
    val studentErrorMessage: String? = null,
    val isSavingStudent: Boolean = false,
    val deviceName: String = "",
    val deviceNameErrorMessage: String? = null,
    val aiDraft: AiConfigurationDraftState = AiConfigurationDraftState(),
    val isValidatingAi: Boolean = false,
    val aiValidationMessage: String? = null,
    val isAiConfigurationActive: Boolean = false,
    val showPrivateHttpRiskConfirmation: Boolean = false,
    val promptEditor: PromptEditorState = PromptEditorState(),
    val showAiDataConsent: Boolean = false,
    val dataManagement: DataManagementState = DataManagementState(),
)

data class DataManagementState(
    val confirmationText: String = "",
    val showClearLearningConfirmation: Boolean = false,
    val isWorking: Boolean = false,
    val errorMessage: String? = null,
    val statusMessage: String? = null,
)

data class PromptEditorState(
    val type: PromptType = PromptType.ANALYZE_IMAGE,
    val template: String = PromptDefaults.getValue(PromptType.ANALYZE_IMAGE),
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val statusMessage: String? = null,
)

data class StudentProfileDraft(
    val nickname: String = "",
    val educationStage: String? = null,
    val enrollmentYear: String = "",
)

val EducationStages = listOf("小学", "初中", "高中")

data class AiConfigurationDraftState(
    val providerType: AiProviderType = AiProviderType.GEMINI,
    val displayName: String = "Gemini",
    val baseUrl: String = GEMINI_DEFAULT_BASE_URL,
    val apiKeyInput: String = "",
    val hasSavedApiKey: Boolean = false,
    val analysisModel: String = "",
    val tutorModel: String = "",
    val allowPrivateCleartext: Boolean = false,
)

const val GEMINI_DEFAULT_BASE_URL = "https://generativelanguage.googleapis.com/v1beta"
const val OPENAI_COMPATIBLE_DEFAULT_BASE_URL = "https://api.openai.com/v1"

sealed interface ProfileAction {
    data class OpenSection(val section: ProfileSection) : ProfileAction

    data object Back : ProfileAction

    data class UpdateNickname(val value: String) : ProfileAction

    data class UpdateEducationStage(val value: String?) : ProfileAction

    data class UpdateEnrollmentYear(val value: String) : ProfileAction

    data object SaveStudentProfile : ProfileAction

    data class UpdateDeviceName(val value: String) : ProfileAction

    data class SelectAiProvider(val value: AiProviderType) : ProfileAction

    data class UpdateAiDisplayName(val value: String) : ProfileAction

    data class UpdateAiBaseUrl(val value: String) : ProfileAction

    data class UpdateAiApiKey(val value: String) : ProfileAction

    data class UpdateAnalysisModel(val value: String) : ProfileAction

    data class UpdateTutorModel(val value: String) : ProfileAction

    data object SaveAiConfiguration : ProfileAction

    data class RequestPrivateHttp(val enabled: Boolean) : ProfileAction

    data object ConfirmPrivateHttp : ProfileAction

    data object DismissPrivateHttp : ProfileAction

    data class SelectPromptType(val type: PromptType) : ProfileAction

    data class UpdatePromptTemplate(val value: String) : ProfileAction

    data object SavePrompt : ProfileAction

    data object ResetPrompt : ProfileAction

    data object RequestAiDataConsent : ProfileAction

    data object ConfirmAiDataConsent : ProfileAction

    data object DismissAiDataConsent : ProfileAction

    data object RequestClearLearningData : ProfileAction

    data class UpdateDataConfirmationText(val value: String) : ProfileAction

    data object ConfirmClearLearningData : ProfileAction

    data object DismissDataConfirmation : ProfileAction
}

sealed interface ProfileEffect {
    data object AiDataConsentGranted : ProfileEffect
}

val PromptDefaults = mapOf(
    PromptType.ANALYZE_IMAGE to
        "{{language_instruction}}\n{{knowledge_points_list}}\n" +
        "{{grade_instruction}}\n{{provider_hints}}",
    PromptType.TUTOR to
        "{{question_context}}\n{{conversation_context}}\n" +
        "{{user_message}}\n{{grade_instruction}}",
    PromptType.GENERATE_EXERCISE to
        "{{original_question}}\n{{knowledge_points}}\n" +
        "{{difficulty_level}}\n{{grade_instruction}}",
    PromptType.GRADE_EXERCISE to
        "{{exercise_question}}\n{{expected_answer}}\n" +
        "{{user_answer}}\n{{rubric_context}}",
)
