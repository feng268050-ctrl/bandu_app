package com.bandu.tiji.feature.capture

import com.bandu.tiji.core.model.erroritem.StoredImage
import java.security.MessageDigest

data class CaptureImageProcessInput(
    val draftId: String,
    val sourceUri: String,
    val rotationDegrees: Int,
)

data class ProcessedCaptureImage(
    val storedImage: StoredImage,
    val imageBytes: ByteArray,
    val mimeType: String = "image/jpeg",
    val jpegQuality: Int,
    val reachedMinimumQuality: Boolean,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ProcessedCaptureImage) return false
        return storedImage == other.storedImage &&
            imageBytes.contentEquals(other.imageBytes) &&
            mimeType == other.mimeType &&
            jpegQuality == other.jpegQuality &&
            reachedMinimumQuality == other.reachedMinimumQuality
    }

    override fun hashCode(): Int {
        var result = storedImage.hashCode()
        result = 31 * result + imageBytes.contentHashCode()
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + jpegQuality
        result = 31 * result + reachedMinimumQuality.hashCode()
        return result
    }
}

interface ProcessCaptureImageUseCase {
    suspend operator fun invoke(
        input: CaptureImageProcessInput,
        onProgress: suspend (Int) -> Unit,
    ): ProcessedCaptureImage
}

class DeterministicCaptureImageProcessor : ProcessCaptureImageUseCase {
    override suspend fun invoke(
        input: CaptureImageProcessInput,
        onProgress: suspend (Int) -> Unit,
    ): ProcessedCaptureImage {
        onProgress(20)
        val bytes = "${input.sourceUri}|${input.rotationDegrees}".encodeToByteArray()
        onProgress(80)
        return ProcessedCaptureImage(
            storedImage = StoredImage(
                relativePath = "images/${input.draftId}.jpg",
                sha256Hex = bytes.sha256Hex(),
                width = if (input.rotationDegrees == 90 || input.rotationDegrees == 270) 1080 else 1440,
                height = if (input.rotationDegrees == 90 || input.rotationDegrees == 270) 1440 else 1080,
                thumbnailRelativePath = "thumbs/${input.draftId}.jpg",
            ),
            imageBytes = bytes,
            jpegQuality = 85,
            reachedMinimumQuality = false,
        ).also {
            onProgress(100)
        }
    }
}

private fun ByteArray.sha256Hex(): String =
    MessageDigest.getInstance("SHA-256")
        .digest(this)
        .joinToString(separator = "") { byte -> "%02x".format(byte) }
