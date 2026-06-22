package com.bandu.tiji.feature.capture

sealed interface CaptureStage {
    data object SelectSource : CaptureStage

    data class Crop(
        val draftId: String,
        val tempUri: String,
        val rotationDegrees: Int = 0,
    ) : CaptureStage {
        init {
            require(draftId.isNotBlank()) { "draftId must not be blank" }
            require(tempUri.isNotBlank()) { "tempUri must not be blank" }
            require(rotationDegrees in VALID_ROTATIONS) {
                "rotationDegrees must be 0, 90, 180, or 270"
            }
        }
    }

    data class Processing(
        val draftId: String,
        val progress: Int,
    ) : CaptureStage {
        init {
            require(draftId.isNotBlank()) { "draftId must not be blank" }
            require(progress in 0..100) { "progress must be between 0 and 100" }
        }
    }

    data class Analyzing(
        val draftId: String,
    ) : CaptureStage {
        init {
            require(draftId.isNotBlank()) { "draftId must not be blank" }
        }
    }

    data class Reviewing(
        val draftId: String,
    ) : CaptureStage {
        init {
            require(draftId.isNotBlank()) { "draftId must not be blank" }
        }
    }

    data class Saving(
        val draftId: String,
    ) : CaptureStage {
        init {
            require(draftId.isNotBlank()) { "draftId must not be blank" }
        }
    }

    private companion object {
        val VALID_ROTATIONS = setOf(0, 90, 180, 270)
    }
}

data class CaptureUiState(
    val stage: CaptureStage = CaptureStage.SelectSource,
    val errorMessage: String? = null,
)

sealed interface CaptureAction {
    data object Cancel : CaptureAction
}

sealed interface CaptureEffect {
    data object NavigateBack : CaptureEffect
}
