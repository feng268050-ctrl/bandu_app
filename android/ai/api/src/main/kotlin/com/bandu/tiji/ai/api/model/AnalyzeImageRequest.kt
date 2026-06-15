package com.bandu.tiji.ai.api.model

data class AnalyzeImageRequest(
    val imageBytes: ByteArray,
    val mimeType: String = "image/jpeg",
    val gradeInstruction: String = "",
    val knowledgePointsList: String = "",
    val languageInstruction: String = "",
    val providerHints: String = "",
) {
    init {
        require(imageBytes.isNotEmpty()) { "imageBytes must not be empty" }
        require(mimeType.isNotBlank()) { "mimeType must not be blank" }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AnalyzeImageRequest) return false
        return imageBytes.contentEquals(other.imageBytes) &&
            mimeType == other.mimeType &&
            gradeInstruction == other.gradeInstruction &&
            knowledgePointsList == other.knowledgePointsList &&
            languageInstruction == other.languageInstruction &&
            providerHints == other.providerHints
    }

    override fun hashCode(): Int {
        var result = imageBytes.contentHashCode()
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + gradeInstruction.hashCode()
        result = 31 * result + knowledgePointsList.hashCode()
        result = 31 * result + languageInstruction.hashCode()
        result = 31 * result + providerHints.hashCode()
        return result
    }
}
