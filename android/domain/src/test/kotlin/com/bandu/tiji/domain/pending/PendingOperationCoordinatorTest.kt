package com.bandu.tiji.domain.pending

import com.bandu.tiji.core.model.id.TutorSessionId
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PendingOperationCoordinatorTest {
    @Test
    fun onConfigurationValidated_resumesOnlyOnce() {
        val coordinator = PendingOperationCoordinator()
        val operation = PendingAiOperation.SendTutorMessage(
            sessionId = TutorSessionId("s1"),
            text = "继续讲解",
        )
        coordinator.save(operation)

        var resumeCount = 0
        val handler: (PendingAiOperation) -> Unit = {
            resumeCount += 1
        }

        coordinator.onConfigurationValidated(handler)
        coordinator.onConfigurationValidated(handler)

        assertThat(resumeCount).isEqualTo(1)
        assertThat(coordinator.peek()).isEqualTo(operation)
    }

    @Test
    fun clear_removesPendingAndAllowsNewResume() {
        val coordinator = PendingOperationCoordinator()
        coordinator.save(PendingAiOperation.AnalyzeCapture("draft-1"))
        coordinator.onConfigurationValidated { }
        coordinator.clear()

        var resumed = false
        coordinator.save(PendingAiOperation.AnalyzeCapture("draft-2"))
        coordinator.onConfigurationValidated { resumed = true }

        assertThat(resumed).isTrue()
        assertThat(coordinator.consumeForResume()).isNull()
    }
}
