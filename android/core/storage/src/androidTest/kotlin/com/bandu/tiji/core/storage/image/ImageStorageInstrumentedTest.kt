package com.bandu.tiji.core.storage.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Rect
import android.media.ExifInterface
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ImageStorageInstrumentedTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val root = File(context.cacheDir, "image-storage-test-${System.nanoTime()}")
    private val storage = ImageStorage(File(root, "images"))

    @After
    fun tearDown() {
        root.deleteRecursively()
    }

    @Test
    fun writeJpegRotatesByExifCropsRemovesExifAndReturnsSha256() {
        val source = File(root, "source.jpg")
        createJpeg(source, width = 32, height = 16, color = Color.RED)
        ExifInterface(source.absolutePath).run {
            setAttribute(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_ROTATE_90.toString(),
            )
            saveAttributes()
        }

        val stored = storage.writeJpeg(
            sourceFile = source,
            relativePath = "items/a.jpg",
            cropRect = Rect(0, 0, 8, 16),
            quality = 90,
        )

        val output = File(root, "images/items/a.jpg")
        val decoded = BitmapFactory.decodeFile(output.absolutePath)
        assertThat(stored.relativePath).isEqualTo("items/a.jpg")
        assertThat(stored.width).isEqualTo(8)
        assertThat(stored.height).isEqualTo(16)
        assertThat(decoded.width).isEqualTo(8)
        assertThat(decoded.height).isEqualTo(16)
        assertThat(stored.sha256Hex).isEqualTo(output.sha256Hex())
        assertThat(
            ExifInterface(output.absolutePath).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED,
            ),
        ).isEqualTo(ExifInterface.ORIENTATION_UNDEFINED)
    }

    @Test
    fun writeJpegAtomicallyReplacesFileAndLeavesNoTempFile() {
        val source = File(root, "source.jpg")
        createJpeg(source, width = 10, height = 10, color = Color.BLUE)

        val first = storage.writeJpeg(source, "items/a.jpg")
        createJpeg(source, width = 12, height = 8, color = Color.GREEN)
        val second = storage.writeJpeg(source, "items/a.jpg")

        val output = File(root, "images/items/a.jpg")
        assertThat(second.width).isEqualTo(12)
        assertThat(second.height).isEqualTo(8)
        assertThat(second.sha256Hex).isEqualTo(output.sha256Hex())
        assertThat(second.sha256Hex).isNotEqualTo(first.sha256Hex)
        assertThat(File(output.parentFile, "${output.name}.tmp").exists()).isFalse()
    }

    @Test
    fun writeJpegCreates480PxThumbnailWithoutUpscalingOriginal() {
        val source = File(root, "source.jpg")
        createJpeg(source, width = 960, height = 540, color = Color.CYAN)

        val stored = storage.writeJpeg(
            sourceFile = source,
            relativePath = "items/a.jpg",
            thumbnailRelativePath = "thumbnails/a.jpg",
        )

        val output = File(root, "images/items/a.jpg")
        val thumbnail = File(root, "images/thumbnails/a.jpg")
        val decodedOutput = BitmapFactory.decodeFile(output.absolutePath)
        val decodedThumbnail = BitmapFactory.decodeFile(thumbnail.absolutePath)
        assertThat(stored.width).isEqualTo(960)
        assertThat(stored.height).isEqualTo(540)
        assertThat(stored.thumbnailRelativePath).isEqualTo("thumbnails/a.jpg")
        assertThat(decodedOutput.width).isEqualTo(960)
        assertThat(decodedOutput.height).isEqualTo(540)
        assertThat(decodedThumbnail.width).isEqualTo(480)
        assertThat(decodedThumbnail.height).isEqualTo(270)
    }

    @Test
    fun cleanupQueueKeepsFailedDeletesAndRetriesLater() {
        val imageRoot = File(root, "images")
        val queue = ImageOrphanCleanupQueue(
            imageRoot = imageRoot,
            queueFile = File(root, "orphan-queue.txt"),
        )
        val deletedImage = File(imageRoot, "items/delete.jpg")
        val blockedTarget = File(imageRoot, "items/stuck.jpg")
        createJpeg(deletedImage, width = 8, height = 8, color = Color.YELLOW)
        require(blockedTarget.mkdirs())
        File(blockedTarget, "child").writeText("not empty")

        queue.enqueue("items/delete.jpg")
        queue.enqueue("items/stuck.jpg")
        queue.enqueue("items/stuck.jpg")
        val firstDrain = queue.drain()

        assertThat(firstDrain.deletedRelativePaths).containsExactly("items/delete.jpg")
        assertThat(firstDrain.pendingRelativePaths).containsExactly("items/stuck.jpg")
        assertThat(queue.pendingRelativePaths()).containsExactly("items/stuck.jpg")
        assertThat(deletedImage.exists()).isFalse()
        assertThat(blockedTarget.exists()).isTrue()

        assertThat(File(blockedTarget, "child").delete()).isTrue()
        val secondDrain = queue.drain()

        assertThat(secondDrain.deletedRelativePaths).containsExactly("items/stuck.jpg")
        assertThat(secondDrain.pendingRelativePaths).isEmpty()
        assertThat(queue.pendingRelativePaths()).isEmpty()
        assertThat(blockedTarget.exists()).isFalse()
    }

    @Test
    fun writeJpegRejectsUnsafePathsAndInvalidCrop() {
        val source = File(root, "source.jpg")
        createJpeg(source, width = 10, height = 10, color = Color.RED)

        org.junit.Assert.assertThrows(ImageStorageException::class.java) {
            storage.writeJpeg(source, "../escape.jpg")
        }
        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            storage.writeJpeg(source, "items/a.jpg", Rect(0, 0, 20, 20))
        }
    }

    private fun createJpeg(
        file: File,
        width: Int,
        height: Int,
        color: Int,
    ) {
        require(file.parentFile?.let { it.exists() || it.mkdirs() } != false)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(color)
        FileOutputStream(file).use { output ->
            assertThat(bitmap.compress(Bitmap.CompressFormat.JPEG, 95, output)).isTrue()
        }
        bitmap.recycle()
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
}
