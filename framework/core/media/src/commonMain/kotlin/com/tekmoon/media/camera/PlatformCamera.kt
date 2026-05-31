package com.tekmoon.media.camera

import androidx.compose.runtime.Composable
import com.tekmoon.media.picker.PickedMediaData

enum class CameraType { Front, Back }
enum class CameraMode { Photo, Video }

@Composable
expect fun rememberCameraLauncher(
    cameraType: CameraType = CameraType.Back,
    cameraMode: CameraMode = CameraMode.Photo,
    returnBytes: Boolean = true,           // if true, try to include bytes for capture
    maxBytesToReturn: Long = 10_000_000L,  // ~10MB guard; larger items come with bytes = null
    onResult: (requestKey: String?, PickedMediaData?) -> Unit,
): CameraLauncher

class CameraLauncher(
    private val onLaunch: (requestKey: String?, fileProviderAuthority: String) -> Unit,
) {
    fun launch(requestKey: String? = null, fileProviderAuthority: String) =
        onLaunch(requestKey, fileProviderAuthority)
}
