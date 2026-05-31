package com.tekmoon.media.bitmap

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.tekmoon.designsystem.platform.PlatformContext
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image as SkiaImage
import platform.Foundation.NSData
import platform.Foundation.NSURL
import platform.Foundation.dataWithContentsOfURL
import platform.posix.memcpy

/**
 * iOS implementations of the bitmap utilities.
 *
 * Uses Skia (bundled with Compose Multiplatform) for decoding / encoding so no UIKit code is
 * needed for the basic image-bytes path. The dominant-color extraction is stubbed — implementing
 * it with `CIAreaAverage` is non-trivial and not blocking; callers get [Color.Black] as a
 * sensible fallback until that work is done.
 */

@OptIn(ExperimentalForeignApi::class)
actual suspend fun loadImageBitmap(source: ImageSource, context: Any?): ImageBitmap? =
    withContext(Dispatchers.Default) {
        try {
            val bytes: ByteArray = when (source) {
                is ImageSource.Bytes -> source.value
                is ImageSource.Url -> {
                    val url = NSURL.URLWithString(source.value) ?: return@withContext null
                    NSData.dataWithContentsOfURL(url)?.toByteArray()
                }
                is ImageSource.PlatformUri -> {
                    val url = NSURL.URLWithString(source.value) ?: return@withContext null
                    NSData.dataWithContentsOfURL(url)?.toByteArray()
                }
            } ?: return@withContext null

            SkiaImage.makeFromEncoded(bytes).toComposeImageBitmap()
        } catch (@Suppress("TooGenericExceptionCaught") _: Exception) {
            // Skia decoding can throw a variety of low-level errors (malformed
            // bytes, OOM, unsupported format). Any failure should fall back to
            // null rather than crash the caller.
            null
        }
    }

actual fun encodeImageBitmapToPng(image: ImageBitmap): ByteArray {
    val skiaBitmap = image.asSkiaBitmap()
    val data = SkiaImage.makeFromBitmap(skiaBitmap).encodeToData(EncodedImageFormat.PNG)
        ?: return ByteArray(0)
    return data.bytes
}

actual suspend fun getDominantColorFromUrl(
    url: String,
    context: PlatformContext,
): Color {
    // TODO: Implement with CIAreaAverage + CIImage averaging.
    return Color.Black
}

actual suspend fun getDominantColorFromBitmap(
    bitmap: ImageBitmap,
    context: PlatformContext,
): Color {
    // TODO: Implement with CIAreaAverage on the underlying CGImage.
    return Color.Black
}

actual fun compressImageToJpeg(
    bytes: ByteArray,
    maxDimension: Int,
    quality: Int,
): ByteArray {
    return try {
        val image = SkiaImage.makeFromEncoded(bytes)
        val w = image.width
        val h = image.height
        val longest = maxOf(w, h)
        val scaled = if (longest > maxDimension) {
            val factor = maxDimension.toFloat() / longest
            val newW = (w * factor).toInt().coerceAtLeast(1)
            val newH = (h * factor).toInt().coerceAtLeast(1)
            // Resize via Skia surface
            val surface = org.jetbrains.skia.Surface.makeRasterN32Premul(newW, newH)
            surface.canvas.drawImageRect(
                image,
                org.jetbrains.skia.Rect.makeWH(w.toFloat(), h.toFloat()),
                org.jetbrains.skia.Rect.makeWH(newW.toFloat(), newH.toFloat()),
                null,
            )
            surface.makeImageSnapshot()
        } else {
            image
        }
        val q = quality.coerceIn(0, 100)
        scaled.encodeToData(EncodedImageFormat.JPEG, q)?.bytes ?: bytes
    } catch (@Suppress("TooGenericExceptionCaught") _: Exception) {
        // Skia encode/scale can throw on bad input or memory pressure. Falling
        // back to the original bytes lets the caller still produce something.
        bytes
    }
}

/** Copy bytes out of an NSData blob. */
@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val size = this.length.toInt()
    val bytes = ByteArray(size)
    if (size == 0) return bytes
    bytes.usePinned { pinned ->
        memcpy(pinned.addressOf(0), this.bytes, this.length)
    }
    return bytes
}
