package com.bandu.tiji.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BanduTokenTest {
    @Test
    fun `light palette matches product visual contract`() {
        assertThat(BanduColors.Background).isEqualTo(Color.White)
        assertThat(BanduColors.SurfaceSubtle).isEqualTo(Color(0xFFF5F5F5))
        assertThat(BanduColors.TextPrimary).isEqualTo(Color(0xFF252525))
        assertThat(BanduColors.TextSecondary).isEqualTo(Color(0xFF737373))
        assertThat(BanduColors.Danger).isEqualTo(Color(0xFFD32F2F))
    }

    @Test
    fun `card dimensions match detailed design`() {
        assertThat(BanduRadii.Card).isEqualTo(12.dp)
        assertThat(BanduBorders.Standard).isEqualTo(1.dp)
        assertThat(BanduElevation.Card).isEqualTo(1.dp)
        assertThat(BanduSpacing.CardGap).isEqualTo(12.dp)
        assertThat(BanduSpacing.PageHorizontal).isEqualTo(16.dp)
    }

    @Test
    fun `spacing scale is strictly increasing`() {
        val scale = listOf(
            BanduSpacing.ExtraSmall,
            BanduSpacing.Small,
            BanduSpacing.CardGap,
            BanduSpacing.PageHorizontal,
            BanduSpacing.Large,
            BanduSpacing.ExtraLarge,
        )

        assertThat(scale.zipWithNext().all { (left, right) -> left < right }).isTrue()
    }
}
