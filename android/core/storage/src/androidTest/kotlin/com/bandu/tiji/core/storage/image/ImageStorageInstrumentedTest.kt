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
