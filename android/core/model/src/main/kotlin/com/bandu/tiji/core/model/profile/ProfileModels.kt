package com.bandu.tiji.core.model.profile

import com.bandu.tiji.core.model.enums.AiProviderType

data class StudentProfile(
    val nickname: String,
    val educationStage: String?,
    val enrollmentYear: Int?,
)

data class AiConfigurationSummary(
    val providerType: AiProviderType,
    val displayName: String,
    val baseUrl: String,
    val analysisModel: String,
    val tutorModel: String,
    val hasApiKey: Boolean,
)

data class DeviceSummary(
    val deviceId: String,
    val displayName: String,
)
