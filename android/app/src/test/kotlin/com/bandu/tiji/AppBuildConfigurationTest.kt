package com.bandu.tiji

import android.content.ComponentName
import android.content.pm.ActivityInfo
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AppBuildConfigurationTest {
    @Test
    fun `application identity and supported SDKs match the product contract`() {
        val application = RuntimeEnvironment.getApplication()
        val applicationInfo = application.applicationInfo

        assertThat(BuildConfig.APPLICATION_ID).isEqualTo("com.bandu.tiji")
        assertThat(BuildConfig.VERSION_CODE).isEqualTo(1001)
        assertThat(BuildConfig.VERSION_NAME).isEqualTo("v0.1.1")
        assertThat(applicationInfo.minSdkVersion).isEqualTo(31)
        assertThat(applicationInfo.targetSdkVersion).isEqualTo(36)
    }

    @Test
    fun `main activity is locked to portrait`() {
        val application = RuntimeEnvironment.getApplication()
        @Suppress("DEPRECATION")
        val activityInfo = application.packageManager.getActivityInfo(
            ComponentName(application, MainActivity::class.java),
            0,
        )

        assertThat(activityInfo.screenOrientation)
            .isEqualTo(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
    }
}
