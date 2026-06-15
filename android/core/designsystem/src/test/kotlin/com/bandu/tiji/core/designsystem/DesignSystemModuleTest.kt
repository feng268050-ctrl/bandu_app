package com.bandu.tiji.core.designsystem

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DesignSystemModuleTest {
    @Test
    fun `module publishes stable design system metadata`() {
        assertThat(DesignSystemModule.Namespace).isEqualTo("com.bandu.tiji.core.designsystem")
        assertThat(DesignSystemModule.MinimumTouchTargetDp).isEqualTo(48)
    }
}
