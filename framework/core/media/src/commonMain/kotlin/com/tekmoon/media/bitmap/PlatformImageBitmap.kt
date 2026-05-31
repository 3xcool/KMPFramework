package com.tekmoon.media.bitmap

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import com.tekmoon.designsystem.platform.PlatformContext

/**
 * Loads an [ImageBitmap] from the given [source].
 *
 * On Android, [context] should be a `coil3.PlatformContext` (alias of `android.content.Context`)
 * when [source] is [ImageSource.PlatformUri].
 */
expect suspend fun loadImageBitmap(source: ImageSource, context: Any?): ImageBitmap?

/** PNG encoding (expect/actual). */
expect fun encodeImageBitmapToPng(image: ImageBitmap): ByteArray

expect suspend fun getDominantColorFromUrl(
    url: String,
    context: PlatformContext,
): Color

expect suspend fun getDominantColorFromBitmap(
    bitmap: ImageBitmap,
    context: PlatformContext,
): Color

/**
 * Decodes [bytes] (any format, e.g. PNG from an image cropper), scales down so the longest
 * edge is at most [maxDimension], then re-encodes as JPEG at [quality] (0–100).
 *
 * Returns the original bytes unchanged on non-Android platforms.
 */
expect fun compressImageToJpeg(
    bytes: ByteArray,
    maxDimension: Int = 1080,
    quality: Int = 85,
): ByteArray
