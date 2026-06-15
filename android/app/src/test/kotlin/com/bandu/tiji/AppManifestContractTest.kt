package com.bandu.tiji

import android.Manifest
import android.content.ComponentName
import android.content.pm.ServiceInfo
import android.content.pm.PackageManager
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AppManifestContractTest {
    @Test
    fun `manifest requests only the required product permissions`() {
        val packageInfo = RuntimeEnvironment.getApplication().packageManager.getPackageInfo(
            BuildConfig.APPLICATION_ID,
            @Suppress("DEPRECATION") android.content.pm.PackageManager.GET_PERMISSIONS,
        )
        val requestedPermissions = packageInfo.requestedPermissions.orEmpty().toSet()

        assertThat(requestedPermissions).containsAtLeast(
            Manifest.permission.CAMERA,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.CHANGE_WIFI_MULTICAST_STATE,
            Manifest.permission.FOREGROUND_SERVICE,
            Manifest.permission.FOREGROUND_SERVICE_DATA_SYNC,
            Manifest.permission.POST_NOTIFICATIONS,
        )
        assertThat(requestedPermissions).containsNoneOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_MEDIA_IMAGES,
        )
    }

    @Test
    fun `transfer service is private and restricted to data sync`() {
        val application = RuntimeEnvironment.getApplication()
        @Suppress("DEPRECATION")
        val serviceInfo = application.packageManager.getServiceInfo(
            ComponentName(
                BuildConfig.APPLICATION_ID,
                "com.bandu.tiji.transfer.runtime.TransferForegroundService",
            ),
            0,
        )

        assertThat(serviceInfo.exported).isFalse()
        assertThat(serviceInfo.foregroundServiceType)
            .isEqualTo(ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
    }

    @Test
    fun `camera hardware stays optional because photo picker is a supported fallback`() {
        val packageInfo = RuntimeEnvironment.getApplication().packageManager.getPackageInfo(
            BuildConfig.APPLICATION_ID,
            @Suppress("DEPRECATION") PackageManager.GET_CONFIGURATIONS,
        )
        val cameraFeature = packageInfo.reqFeatures.orEmpty()
            .single { it.name == PackageManager.FEATURE_CAMERA }

        assertThat(cameraFeature.flags and android.content.pm.FeatureInfo.FLAG_REQUIRED)
            .isEqualTo(0)
    }
}
