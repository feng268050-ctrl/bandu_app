package com.bandu.tiji.core.model.enums

enum class MasteryLevel(val storageValue: Int) {
    NEW(0),
    REVIEWING(1),
    MASTERED(2),
    ;

    companion object {
        fun fromStorageValue(value: Int): MasteryLevel =
            entries.firstOrNull { it.storageValue == value }
                ?: error("Unknown mastery level: $value")
    }
}

enum class MistakeStatus {
    WRONG_ATTEMPT,
    NOT_ATTEMPTED,
    UNKNOWN,
}

enum class PaperLevel {
    A,
    B,
    OTHER,
}

enum class ExerciseDifficulty {
    EASY,
    MEDIUM,
    HARD,
    CHALLENGE,
}

enum class GradeResult {
    CORRECT,
    INCORRECT,
    NEEDS_REVIEW,
}

enum class AiProviderType {
    GEMINI,
    OPENAI_COMPATIBLE,
}
