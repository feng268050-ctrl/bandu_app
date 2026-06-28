package com.bandu.tiji.feature.capture

import com.google.common.truth.Truth.assertThat
import java.io.File
import org.junit.Test

class PhotoPickerManifestTest {
    @Test
    fun `capture only declares camera permission and no broad media read permission`() {
        val manifest = findAndroidRoot()
            .resolve("feature/capture/src/main/AndroidManifest.xml")
            .readText()

        assertThat(manifest).contains("android.permission.CAMERA")
        assertThat(manifest).doesNotContain("android.permission.READ_MEDIA_IMAGES")
        assertThat(manifest).doesNotContain("android.permission.READ_MEDIA_VIDEO")
        assertThat(manifest).doesNotContain("android.permission.READ_EXTERNAL_STORAGE")
        assertThat(manifest).doesNotContain("android.permission.WRITE_EXTERNAL_STORAGE")
    }

    private fun findAndroidRoot(): File {
        var current = File(requireNotNull(System.getProperty("user.dir"))).canonicalFile
        while (true) {
            if (current.resolve("settings.gradle.kts").isFile &&
                current.resolve("feature/capture").isDirectory
            ) {
                return current
            }
            current = current.parentFile
                ?: error("Unable to locate Android project root")
        }
    }
}
