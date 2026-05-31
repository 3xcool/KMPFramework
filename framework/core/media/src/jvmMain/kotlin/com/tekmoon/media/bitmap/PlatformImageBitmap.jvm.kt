package com.tekmoon.media.bitmap

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import com.tekmoon.designsystem.platform.PlatformContext

/**
 * Desktop JVM implementations.
 *
 * TODO: Implement using `BufferedImage` / `ImageIO`.
 */

actual suspend fun loadImageBitmap(source: ImageSource, context: Any?): ImageBitmap? = null

actual fun encodeImageBitmapToPng(image: ImageBitmap): ByteArray = ByteArray(0)

actual suspend fun getDominantColorFromUrl(
    url: String,
    context: PlatformContext,
): Color = Color.Black

actual suspend fun getDominantColorFromBitmap(
    bitmap: ImageBitmap,
    context: PlatformContext,
): Color = Color.Black

actual fun compressImageToJpeg(
    bytes: ByteArray,
    maxDimension: Int,
    quality: Int,
): ByteArray = bytes
