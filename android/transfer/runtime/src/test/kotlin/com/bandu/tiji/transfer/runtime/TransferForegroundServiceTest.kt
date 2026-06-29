package com.bandu.tiji.transfer.runtime

import com.google.common.truth.Truth.assertThat
import org.junit.Test

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
}
