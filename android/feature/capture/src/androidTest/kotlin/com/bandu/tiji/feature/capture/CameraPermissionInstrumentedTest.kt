package com.bandu.tiji.feature.capture

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class CameraPermissionInstrumentedTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun grantedPermissionShowsCameraCapture() {
        composeRule.setContent {
            CameraPermissionGate(
                permissionGranted = true,
                onRequestPermission = {},
                onCaptured = {},
                onChoosePhoto = {},
                onCancel = {},
            )
        }

        composeRule.onNodeWithTag(CAMERA_PERMISSION_GRANTED_TAG).assertExists()
        composeRule.onNodeWithTag(CAMERA_PREVIEW_TAG).assertExists()
        composeRule.onNodeWithTag(CAMERA_SHUTTER_TAG).assertExists()
    }

    @Test
    fun deniedPermissionKeepsPhotoPickerAvailable() {
        var chosePhoto = false
        composeRule.setContent {
            CameraPermissionGate(
                permissionGranted = false,
                onRequestPermission = {},
                onCaptured = {},
                onChoosePhoto = { chosePhoto = true },
                onCancel = {},
            )
        }

        composeRule.onNodeWithTag(CAMERA_PERMISSION_DENIED_TAG).assertExists()
        composeRule.onNodeWithTag(CAMERA_DENIED_PHOTO_TAG).performClick()
        composeRule.runOnIdle {
            check(chosePhoto)
        }
    }
}
