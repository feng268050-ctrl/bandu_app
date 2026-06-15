package com.bandu.tiji.core.model.id

@JvmInline
value class CollectionId(val value: String) {
    init {
        require(value.isNotBlank()) { "CollectionId must not be blank" }
    }
}

@JvmInline
value class ErrorItemId(val value: String) {
    init {
        require(value.isNotBlank()) { "ErrorItemId must not be blank" }
    }
}

@JvmInline
value class TagId(val value: String) {
    init {
        require(value.isNotBlank()) { "TagId must not be blank" }
    }
}

@JvmInline
value class TutorSessionId(val value: String) {
    init {
        require(value.isNotBlank()) { "TutorSessionId must not be blank" }
    }
}

@JvmInline
value class TutorMessageId(val value: String) {
    init {
        require(value.isNotBlank()) { "TutorMessageId must not be blank" }
    }
}

@JvmInline
value class ExerciseId(val value: String) {
    init {
        require(value.isNotBlank()) { "ExerciseId must not be blank" }
    }
}
