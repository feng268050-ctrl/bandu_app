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

class ImageStorage(
    private val imageRoot: File,
) {
    fun writeJpeg(
        sourceFile: File,
        relativePath: String,
        cropRect: Rect? = null,
        quality: Int = DEFAULT_JPEG_QUALITY,
    ): StoredImage {
        require(quality in 1..100) { "JPEG quality must be between 1 and 100" }
        requireSafeRelativePath(relativePath)
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
        try {
            writeBitmapAtomically(target, transformed, quality)
            return StoredImage(
                relativePath = relativePath,
                sha256Hex = target.sha256Hex(),
                width = transformed.width,
                height = transformed.height,
            )
        } finally {
            if (transformed !== source && !transformed.isRecycled) {
                transformed.recycle()
            }
            if (!source.isRecycled) {
                source.recycle()
            }
        }
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
    }
}

class ImageStorageException(
    message: String,
    cause: Throwable? = null,
) : IllegalStateException(message, cause)
