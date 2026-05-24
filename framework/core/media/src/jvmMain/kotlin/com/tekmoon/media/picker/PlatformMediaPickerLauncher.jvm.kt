package com.tekmoon.media.picker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * Desktop JVM implementation of [rememberMediaPickerLauncher].
 *
 * TODO: Use `JFileChooser` / `FileDialog` for desktop file picking.
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
