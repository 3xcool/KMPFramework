package com.tekmoon.media.camera

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.tekmoon.media.picker.PickedMediaData

/**
 * Desktop JVM implementation of [rememberCameraLauncher].
 *
 * No standard camera API on desktop — returns a no-op launcher.
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
