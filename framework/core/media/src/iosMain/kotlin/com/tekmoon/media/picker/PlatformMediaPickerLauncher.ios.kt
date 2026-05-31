package com.tekmoon.media.picker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * iOS implementation of [rememberMediaPickerLauncher].
 *
 * STATUS: Stub. Returns a launcher that immediately invokes [onCancelled] so consumer code
 * doesn't crash. A real implementation requires:
 *  1. Building a `PHPickerConfiguration` from [mediaPickerTypes] + [selectionLimit].
 *  2. Instantiating `PHPickerViewController(configuration:)` and assigning a delegate that
 *     converts `[PHPickerResult]` into [PickedMediaData] (loading file representations via
 *     `itemProvider.loadFileRepresentationForTypeIdentifier`).
 *  3. Presenting the picker from the current root view controller
 *     (`UIApplication.sharedApplication.keyWindow?.rootViewController?.presentViewController(...)`).
 *  4. Honoring [returnBytes] / [maxBytesToReturn] for the bytes payload.
 *
 * Track this as a follow-up before shipping iOS features that depend on media picking.
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
