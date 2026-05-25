package com.tekmoon.media.camera

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.tekmoon.media.picker.PickedMediaData

/**
 * iOS implementation of [rememberCameraLauncher].
 *
 * STATUS: Stub. Returns a launcher that immediately invokes [onResult] with `null` so consumer
 * code doesn't crash. A real implementation requires:
 *  1. Instantiating `UIImagePickerController()` with `sourceType = .camera`,
 *     `cameraDevice = .front/.rear` based on [cameraType], and `mediaTypes` based on [cameraMode]
 *     (`kUTTypeImage` for Photo, `kUTTypeMovie` for Video).
 *  2. Implementing a `UIImagePickerControllerDelegate` that translates the captured asset to
 *     [PickedMediaData] — writing the image / video to a temp file in `NSTemporaryDirectory`,
 *     extracting `width`/`height` (UIImage) or `duration`/`width`/`height` (AVURLAsset).
 *  3. Presenting from the current root view controller and dismissing on completion.
 *  4. Honoring [returnBytes] / [maxBytesToReturn] for the bytes payload.
 *
 * Track this as a follow-up before shipping iOS features that depend on camera capture.
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
