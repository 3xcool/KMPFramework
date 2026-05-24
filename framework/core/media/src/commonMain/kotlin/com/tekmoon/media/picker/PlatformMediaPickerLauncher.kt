package com.tekmoon.media.picker

import androidx.compose.runtime.Composable

enum class MediaPickerType { Image, Video }

/**
 * @param onResult returns the requestKey to help the caller identify the request and a list of [PickedMediaData]
 */
@Composable
expect fun rememberMediaPickerLauncher(
    mediaPickerTypes: Set<MediaPickerType> = setOf(MediaPickerType.Image),
    selectionLimit: Int = 1,               // 0 = unlimited; OS will determine
    returnBytes: Boolean = true,           // if true, try to include bytes for each pick
    maxBytesToReturn: Long = 8_000_000L,   // ~8MB guard
    onCancelled: (requestKey: String?) -> Unit,
    onResult: (requestKey: String?, List<PickedMediaData>) -> Unit,
): MediaPickerLauncher

class MediaPickerLauncher(
    private val onLaunch: (requestKey: String?) -> Unit,
) {
    fun launch(requestKey: String? = null) = onLaunch(requestKey)
}
