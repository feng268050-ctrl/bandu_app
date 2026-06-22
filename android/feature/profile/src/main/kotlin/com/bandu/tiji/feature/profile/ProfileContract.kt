package com.bandu.tiji.feature.profile

import com.bandu.tiji.core.model.enums.AiProviderType

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
}

sealed interface ProfileEffect
