package com.tekmoon.media.camera

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.tekmoon.media.picker.PickedMediaData

/**
 * iOS implementation of [rememberCameraLauncher].
 *
 * TODO: Wire up `UIImagePickerController` / `AVCaptureSession`.
 */
@Composable
actual fun rememberCameraLauncher(
    cameraType: CameraType,
    cameraMode: CameraMode,
    returnBytes: Boolean,
    maxBytesToReturn: Long,
    onResult: (requestKey: String?, PickedMediaData?) -> Unit,
): CameraLauncher = remember {
    CameraLauncher(
        onLaunch = { requestKey, _ -> onResult(requestKey, null) },
    )
}
