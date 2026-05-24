package com.tekmoon.media.picker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * iOS implementation of [rememberMediaPickerLauncher].
 *
 * TODO: Wire up `PHPickerViewController` (iOS 14+) and convert results to [PickedMediaData].
 */
@Composable
actual fun rememberMediaPickerLauncher(
    mediaPickerTypes: Set<MediaPickerType>,
    selectionLimit: Int,
    returnBytes: Boolean,
    maxBytesToReturn: Long,
    onCancelled: (requestKey: String?) -> Unit,
    onResult: (requestKey: String?, List<PickedMediaData>) -> Unit,
): MediaPickerLauncher = remember {
    MediaPickerLauncher(
        onLaunch = { requestKey -> onCancelled(requestKey) },
    )
}
