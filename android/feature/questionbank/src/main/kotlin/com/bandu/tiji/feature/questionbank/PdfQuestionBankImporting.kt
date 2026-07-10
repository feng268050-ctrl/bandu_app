package com.bandu.tiji.feature.questionbank

data class PdfQuestionBankDocument(
    val totalPageCount: Int,
    val pages: List<PdfQuestionBankPage>,
) {
    init {
        require(totalPageCount >= 0) { "totalPageCount must not be negative" }
    }
}

data class PdfQuestionBankPage(
    val pageNumber: Int,
    val imageBytes: ByteArray,
    val mimeType: String = "image/jpeg",
) {
    init {
        require(pageNumber > 0) { "pageNumber must be positive" }
        require(imageBytes.isNotEmpty()) { "imageBytes must not be empty" }
        require(mimeType.isNotBlank()) { "mimeType must not be blank" }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PdfQuestionBankPage) return false
        return pageNumber == other.pageNumber &&
            imageBytes.contentEquals(other.imageBytes) &&
            mimeType == other.mimeType
    }

    override fun hashCode(): Int {
        var result = pageNumber
        result = 31 * result + imageBytes.contentHashCode()
        result = 31 * result + mimeType.hashCode()
        return result
    }
}

fun interface PdfQuestionBankPageExtractor {
    suspend fun extract(uri: String): PdfQuestionBankDocument
}
