package com.bandu.tiji.domain.ai

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AiDataConsentCoordinatorTest {
    @Test
    fun `consent remains accepted until explicitly reset`() {
        val coordinator = AiDataConsentCoordinator()

        assertThat(coordinator.isAccepted()).isFalse()
        coordinator.accept()
        assertThat(coordinator.isAccepted()).isTrue()
        coordinator.reset()
        assertThat(coordinator.isAccepted()).isFalse()
    }
}
