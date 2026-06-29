package com.bandu.tiji.transfer.runtime

import com.bandu.tiji.core.model.transfer.TransferPhase
import com.bandu.tiji.core.model.transfer.TransferProgress
import com.google.common.truth.Truth.assertThat
import java.io.File
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class TransferForegroundServiceTest {
    @Test
    fun `in progress notification clamps progress and stays ongoing`() {
        val content = TransferNotificationContent.inProgress(
            percentComplete = 140,
            bytesText = "2 MB / 5 MB",
        )

        assertThat(content.title).isEqualTo("设备迁移进行中")
        assertThat(content.text).isEqualTo("2 MB / 5 MB")
        assertThat(content.percentComplete).isEqualTo(100)
        assertThat(content.ongoing).isTrue()
    }

    @Test
    fun `verifying notification states original data is retained`() {
        val content = TransferNotificationContent.verifying()

        assertThat(content.text).contains("不会替换原数据")
        assertThat(content.indeterminate).isTrue()
        assertThat(content.ongoing).isTrue()
    }

    @Test
    fun `failed notification is dismissible and keeps target data message`() {
        val content = TransferNotificationContent.failed("")

        assertThat(content.title).isEqualTo("设备迁移失败")
        assertThat(content.text).contains("原数据仍保留")
        assertThat(content.ongoing).isFalse()
    }

    @Test
    fun `start intent is explicit and carries observable progress`() {
        val context = androidx.test.core.app.ApplicationProvider
            .getApplicationContext<android.content.Context>()
        val intent = TransferForegroundService.startIntent(
            context = context,
            progress = TransferProgress(
                phase = TransferPhase.TRANSFERRING,
                percentComplete = 42,
                transferredBytes = 42L,
                totalBytes = 100L,
                bytesPerSecond = 10L,
            ),
            bytesText = "42 B / 100 B",
        )

        assertThat(intent.action).isEqualTo(TransferForegroundService.ACTION_START_TRANSFER)
        assertThat(intent.component?.className).isEqualTo(TransferForegroundService::class.java.name)
        assertThat(intent.getIntExtra(TransferForegroundService.EXTRA_PERCENT, -1)).isEqualTo(42)
        assertThat(intent.getStringExtra(TransferForegroundService.EXTRA_BYTES)).isEqualTo("42 B / 100 B")
    }

    @Test
    fun `runtime source does not declare background schedulers or boot receivers`() {
        val source = findRuntimeMainRoot().walkTopDown()
            .filter { it.isFile && it.extension in setOf("kt", "xml") }
            .joinToString(separator = "\n") { it.readText() }

        listOf(
            "WorkManager",
            "PeriodicWorkRequest",
            "BOOT_COMPLETED",
            "RECEIVE_BOOT_COMPLETED",
            "JobScheduler",
        ).forEach { forbidden ->
            assertThat(source).doesNotContain(forbidden)
        }
    }

    private fun findRuntimeMainRoot(): File {
        var current = File(requireNotNull(System.getProperty("user.dir"))).canonicalFile
        while (true) {
            val runtimeMain = current.resolve("transfer/runtime/src/main")
            if (current.resolve("settings.gradle.kts").isFile && runtimeMain.isDirectory) {
                return runtimeMain
            }
            current = current.parentFile ?: error("Unable to locate Android project root")
        }
    }
}
