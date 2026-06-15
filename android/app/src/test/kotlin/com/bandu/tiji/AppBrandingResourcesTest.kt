package com.bandu.tiji

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AppBrandingResourcesTest {
    @Test
    fun `manifest exposes the product name and launcher icons`() {
        val applicationInfo = RuntimeEnvironment.getApplication().applicationInfo
        val resources = RuntimeEnvironment.getApplication().resources

        assertThat(applicationInfo.loadLabel(RuntimeEnvironment.getApplication().packageManager))
            .isEqualTo("伴读题集")
        assertThat(applicationInfo.icon).isNotEqualTo(0)
        assertThat(resources.getResourceName(applicationInfo.icon))
            .isEqualTo("com.bandu.tiji:mipmap/ic_launcher")
    }

    @Test
    fun `startup theme uses a light non transparent background`() {
        val color = RuntimeEnvironment.getApplication()
            .getColor(R.color.splash_background)

        assertThat(color).isEqualTo(android.graphics.Color.WHITE)
    }
}
