package com.bandu.tiji.transfer.runtime.failure

import com.bandu.tiji.domain.transfer.TransferFailureCode
import com.google.common.truth.Truth.assertThat
import java.io.File
import org.junit.Test

class TransferFailureHandlerTest {
    @Test
    fun `network interruption preserves resume state and is resumable`() {
        val root = File("build/test-transfer-failure/${System.nanoTime()}")
        val sessionDir = File(root, "session-a").apply { mkdirs() }
        File(sessionDir, "resume.pb").writeText("resume")

        val decision = TransferFailureHandler(root).handle(
            sessionId = "session-a",
            cause = RuntimeFailureCause.NETWORK_INTERRUPTED,
        )

        assertThat(decision.state.code).isEqualTo(TransferFailureCode.NETWORK_INTERRUPTED)
        assertThat(decision.state.resumable).isTrue()
        assertThat(decision.cleanup).isEqualTo(CleanupMode.PRESERVE_RESUME)
        assertThat(sessionDir.exists()).isTrue()
    }

    @Test
    fun `integrity failure deletes corrupted session package`() {
        val root = File("build/test-transfer-failure-integrity/${System.nanoTime()}")
        val sessionDir = File(root, "session-b").apply { mkdirs() }
        File(sessionDir, "manifest.pb").writeText("tampered")

        val decision = TransferFailureHandler(root).handle(
            sessionId = "session-b",
            cause = RuntimeFailureCause.INTEGRITY_CHECK_FAILED,
        )

        assertThat(decision.state.code).isEqualTo(TransferFailureCode.CHECKSUM_FAILED)
        assertThat(decision.state.resumable).isFalse()
        assertThat(decision.cleanup).isEqualTo(CleanupMode.DELETE_SESSION)
        assertThat(sessionDir.exists()).isFalse()
    }

    @Test
    fun `commit failure keeps target data for rollback diagnostics`() {
        val root = File("build/test-transfer-failure-commit/${System.nanoTime()}")
        val sessionDir = File(root, "session-c").apply { mkdirs() }
        File(sessionDir, "import.tmp").writeText("pending")

        val decision = TransferFailureHandler(root).handle(
            sessionId = "session-c",
            cause = RuntimeFailureCause.COMMIT_FAILED,
        )

        assertThat(decision.state.code).isEqualTo(TransferFailureCode.COMMIT_FAILED)
        assertThat(decision.state.resumable).isFalse()
        assertThat(decision.cleanup).isEqualTo(CleanupMode.PRESERVE_TARGET_DATA)
        assertThat(sessionDir.exists()).isTrue()
    }

    @Test
    fun `user cancellation deletes temporary transfer files`() {
        val root = File("build/test-transfer-failure-cancel/${System.nanoTime()}")
        val sessionDir = File(root, "session-d").apply { mkdirs() }
        File(sessionDir, "resume.pb").writeText("resume")

        val decision = TransferFailureHandler(root).handle(
            sessionId = "session-d",
            cause = RuntimeFailureCause.USER_CANCELLED,
        )

        assertThat(decision.state.code).isEqualTo(TransferFailureCode.CANCELLED)
        assertThat(decision.cleanup).isEqualTo(CleanupMode.DELETE_SESSION)
        assertThat(sessionDir.exists()).isFalse()
    }
}
