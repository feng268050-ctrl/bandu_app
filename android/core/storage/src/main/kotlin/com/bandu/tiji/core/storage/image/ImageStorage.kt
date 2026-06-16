package com.bandu.tiji.core.storage.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Rect
import android.media.ExifInterface
import com.bandu.tiji.core.model.erroritem.StoredImage
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import kotlin.math.roundToInt

class ImageStorage(
    private val imageRoot: File,
) {
    fun writeJpeg(
        sourceFile: File,
        relativePath: String,
        cropRect: Rect? = null,
        quality: Int = DEFAULT_JPEG_QUALITY,
        thumbnailRelativePath: String? = null,
        thumbnailMaxSidePx: Int = DEFAULT_THUMBNAIL_MAX_SIDE_PX,
    ): StoredImage {
        require(quality in 1..100) { "JPEG quality must be between 1 and 100" }
        require(thumbnailMaxSidePx > 0) { "Thumbnail max side must be positive" }
        requireSafeRelativePath(relativePath)
        thumbnailRelativePath?.let(::requireSafeRelativePath)
        val target = File(imageRoot, relativePath)
        val source = BitmapFactory.decodeFile(sourceFile.absolutePath)
            ?: throw ImageStorageException("Unable to decode image")
        val transformed = try {
            source.applyExifOrientation(readOrientation(sourceFile))
                .let { oriented ->
                    if (cropRect == null) {
                        oriented
                    } else {
                        oriented.crop(cropRect)
                    }
                }
        } catch (error: RuntimeException) {
            if (!source.isRecycled) source.recycle()
            throw error
        }
        var thumbnail: Bitmap? = null
        try {
            writeBitmapAtomically(target, transformed, quality)
            val storedThumbnailPath = if (thumbnailRelativePath == null) {
                null
            } else {
                val thumbnailTarget = File(imageRoot, thumbnailRelativePath)
                thumbnail = transformed.scaleToFit(thumbnailMaxSidePx)
                writeBitmapAtomically(thumbnailTarget, requireNotNull(thumbnail), quality)
                thumbnailRelativePath
            }
            return StoredImage(
                relativePath = relativePath,
                sha256Hex = target.sha256Hex(),
                width = transformed.width,
                height = transformed.height,
                thumbnailRelativePath = storedThumbnailPath,
            )
        } finally {
            val generatedThumbnail = thumbnail
            if (generatedThumbnail != null && generatedThumbnail !== transformed && !generatedThumbnail.isRecycled) {
                generatedThumbnail.recycle()
            }
            if (transformed !== source && !transformed.isRecycled) {
                transformed.recycle()
            }
            if (!source.isRecycled) {
                source.recycle()
            }
        }
    }

    private fun Bitmap.scaleToFit(maxSidePx: Int): Bitmap {
        val longestSide = maxOf(width, height)
        if (longestSide <= maxSidePx) return this
        val scale = maxSidePx.toFloat() / longestSide.toFloat()
        val scaledWidth = (width * scale).roundToInt().coerceAtLeast(1)
        val scaledHeight = (height * scale).roundToInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(this, scaledWidth, scaledHeight, true)
    }

    private fun Bitmap.applyExifOrientation(orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.preScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.preScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            else -> return this
        }
        return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    }

    private fun Bitmap.crop(rect: Rect): Bitmap {
        require(rect.width() > 0 && rect.height() > 0) { "Crop rectangle must not be empty" }
        require(
            rect.left >= 0 &&
                rect.top >= 0 &&
                rect.right <= width &&
                rect.bottom <= height,
        ) {
            "Crop rectangle is outside the image"
        }
        return Bitmap.createBitmap(this, rect.left, rect.top, rect.width(), rect.height())
    }

    private fun readOrientation(sourceFile: File): Int = runCatching {
        ExifInterface(sourceFile.absolutePath).getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL,
        )
    }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

    private fun writeBitmapAtomically(
        target: File,
        bitmap: Bitmap,
        quality: Int,
    ) {
        require(target.parentFile?.let { it.exists() || it.mkdirs() } != false) {
            "Unable to create image directory"
        }
        val temp = File(target.parentFile, "${target.name}.tmp")
        FileOutputStream(temp).use { output ->
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, quality, output)) {
                throw ImageStorageException("Unable to encode JPEG")
            }
            output.fd.sync()
        }
        if (target.exists() && !target.delete()) {
            temp.delete()
            throw ImageStorageException("Unable to replace image")
        }
        if (!temp.renameTo(target)) {
            temp.delete()
            throw ImageStorageException("Unable to commit image")
        }
    }

    private fun File.sha256Hex(): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(this).use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                if (count > 0) digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    companion object {
        const val DEFAULT_JPEG_QUALITY = 88
        const val DEFAULT_THUMBNAIL_MAX_SIDE_PX = 480
    }
}

class ImageStorageException(
    message: String,
    cause: Throwable? = null,
) : IllegalStateException(message, cause)

class ImageOrphanCleanupQueue(
    private val imageRoot: File,
    private val queueFile: File = File(imageRoot, ".orphan-cleanup.queue"),
) {
    fun enqueue(relativePath: String) {
        requireSafeRelativePath(relativePath)
        val pending = pendingRelativePaths().toMutableList()
        if (relativePath !in pending) {
            pending += relativePath
            writeQueue(pending)
        }
    }

    fun drain(): ImageCleanupResult {
        val pending = pendingRelativePaths()
        val deleted = mutableListOf<String>()
        val remaining = mutableListOf<String>()
        for (relativePath in pending) {
            val target = File(imageRoot, relativePath)
            if (!target.exists() || target.delete()) {
                deleted += relativePath
                pruneEmptyParents(target.parentFile)
            } else {
                remaining += relativePath
            }
        }
        writeQueue(remaining)
        return ImageCleanupResult(
            deletedRelativePaths = deleted,
            pendingRelativePaths = remaining,
        )
    }

    fun pendingRelativePaths(): List<String> {
        if (!queueFile.exists()) return emptyList()
        return queueFile.readLines()
            .map(String::trim)
            .filter(String::isNotEmpty)
            .distinct()
            .onEach(::requireSafeRelativePath)
    }

    private fun writeQueue(relativePaths: List<String>) {
        require(queueFile.parentFile?.let { it.exists() || it.mkdirs() } != false) {
            "Unable to create image cleanup queue directory"
        }
        val temp = File(queueFile.parentFile, "${queueFile.name}.tmp")
        FileOutputStream(temp).use { output ->
            output.write(relativePaths.joinToString(separator = "\n").toByteArray(Charsets.UTF_8))
            if (relativePaths.isNotEmpty()) {
                output.write('\n'.code)
            }
            output.fd.sync()
        }
        if (queueFile.exists() && !queueFile.delete()) {
            temp.delete()
            throw ImageStorageException("Unable to replace image cleanup queue")
        }
        if (!temp.renameTo(queueFile)) {
            temp.delete()
            throw ImageStorageException("Unable to commit image cleanup queue")
        }
    }

    private fun pruneEmptyParents(start: File?) {
        var directory = start
        while (
            directory != null &&
            directory != imageRoot &&
            directory.absolutePath.startsWith(imageRoot.absolutePath)
        ) {
            if (!directory.delete()) return
            directory = directory.parentFile
        }
    }
}

data class ImageCleanupResult(
    val deletedRelativePaths: List<String>,
    val pendingRelativePaths: List<String>,
)

private fun requireSafeRelativePath(relativePath: String) {
    val segments = relativePath.split('/')
    if (
        relativePath.isBlank() ||
        relativePath.startsWith('/') ||
        '\\' in relativePath ||
        '\u0000' in relativePath ||
        segments.any { it.isBlank() || it == "." || it == ".." }
    ) {
        throw ImageStorageException("Unsafe image path")
    }
}
