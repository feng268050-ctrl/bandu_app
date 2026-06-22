package com.bandu.tiji.feature.capture

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.bandu.tiji.core.designsystem.theme.BanduSpacing
import java.io.File
import java.util.UUID
import java.util.concurrent.Executor

@Composable
fun CameraCaptureLauncher(
    onCaptured: (String) -> Unit,
    onChoosePhoto: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var permissionGranted by remember {
        mutableStateOf<Boolean?>(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                .takeIf { it == PackageManager.PERMISSION_GRANTED }
                ?.let { true },
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        permissionGranted = granted
    }

    LaunchedEffect(Unit) {
        if (permissionGranted == null) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    CameraPermissionGate(
        permissionGranted = permissionGranted,
        onRequestPermission = {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        },
        onCaptured = onCaptured,
        onChoosePhoto = onChoosePhoto,
        onCancel = onCancel,
        modifier = modifier,
    )
}

@Composable
internal fun CameraPermissionGate(
    permissionGranted: Boolean?,
    onRequestPermission: () -> Unit,
    onCaptured: (String) -> Unit,
    onChoosePhoto: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (permissionGranted) {
        null -> Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text("正在请求相机权限")
        }

        false -> Column(
            modifier = modifier
                .fillMaxSize()
                .padding(BanduSpacing.PageHorizontal)
                .testTag(CAMERA_PERMISSION_DENIED_TAG),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("无法使用相机")
            Text("相机权限未授予，你仍可从相册选择题目图片。")
            Button(onClick = onRequestPermission) {
                Text("重新授权")
            }
            OutlinedButton(
                onClick = onChoosePhoto,
                modifier = Modifier.testTag(CAMERA_DENIED_PHOTO_TAG),
            ) {
                Text("从相册选择")
            }
            OutlinedButton(onClick = onCancel) {
                Text("取消")
            }
        }

        true -> CameraXCaptureContent(
            onCaptured = onCaptured,
            onCancel = onCancel,
            modifier = modifier.testTag(CAMERA_PERMISSION_GRANTED_TAG),
        )
    }
}

@Composable
private fun CameraXCaptureContent(
    onCaptured: (String) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }
    val mainExecutor = remember(context) { ContextCompat.getMainExecutor(context) }
    var cameraError by remember { mutableStateOf<String?>(null) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(BanduSpacing.Small),
    ) {
        AndroidView(
            factory = { previewContext ->
                PreviewView(previewContext).also { previewView ->
                    val providerFuture = ProcessCameraProvider.getInstance(previewContext)
                    providerFuture.addListener(
                        {
                            runCatching {
                                val provider = providerFuture.get()
                                cameraProvider = provider
                                val preview = Preview.Builder().build().also {
                                    it.surfaceProvider = previewView.surfaceProvider
                                }
                                provider.unbindAll()
                                provider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    imageCapture,
                                )
                            }.onFailure {
                                cameraError = "无法启动相机"
                            }
                        },
                        mainExecutor,
                    )
                }
            },
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag(CAMERA_PREVIEW_TAG),
        )

        cameraError?.let { Text(it) }
        Button(
            onClick = {
                capturePhoto(
                    imageCapture = imageCapture,
                    executor = mainExecutor,
                    outputDirectory = File(context.cacheDir, "capture"),
                    onCaptured = onCaptured,
                    onError = { cameraError = "拍照失败，请重试" },
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag(CAMERA_SHUTTER_TAG),
        ) {
            Text("拍照")
        }
        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("取消")
        }
    }

    DisposableEffect(lifecycleOwner) {
        onDispose {
            cameraProvider?.unbindAll()
        }
    }
}

private fun capturePhoto(
    imageCapture: ImageCapture,
    executor: Executor,
    outputDirectory: File,
    onCaptured: (String) -> Unit,
    onError: (ImageCaptureException) -> Unit,
) {
    outputDirectory.mkdirs()
    val outputFile = File(outputDirectory, "${UUID.randomUUID()}.jpg")
    val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()
    imageCapture.takePicture(
        outputOptions,
        executor,
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                onCaptured(Uri.fromFile(outputFile).toString())
            }

            override fun onError(exception: ImageCaptureException) {
                outputFile.delete()
                onError(exception)
            }
        },
    )
}

internal const val CAMERA_PERMISSION_GRANTED_TAG = "camera-permission-granted"
internal const val CAMERA_PERMISSION_DENIED_TAG = "camera-permission-denied"
internal const val CAMERA_DENIED_PHOTO_TAG = "camera-denied-photo"
internal const val CAMERA_PREVIEW_TAG = "camera-preview"
internal const val CAMERA_SHUTTER_TAG = "camera-shutter"
