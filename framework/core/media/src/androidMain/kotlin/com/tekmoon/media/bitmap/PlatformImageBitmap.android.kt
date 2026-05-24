package com.tekmoon.media.bitmap

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.scale
import androidx.palette.graphics.Palette
import com.tekmoon.designsystem.platform.PlatformContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.net.URL
import kotlin.math.roundToInt

actual suspend fun loadImageBitmap(source: ImageSource, context: Any?): ImageBitmap? = when (source) {
    is ImageSource.Url -> withContext(Dispatchers.IO) {
        try {
            val bytes = URL(source.value).readBytes()
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            bitmap?.prepareToDraw()
            bitmap?.asImageBitmap()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    is ImageSource.Bytes -> withContext(Dispatchers.Default) {
        try {
            val bitmap = BitmapFactory.decodeByteArray(source.value, 0, source.value.size)
            bitmap?.prepareToDraw()
            bitmap?.asImageBitmap()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    is ImageSource.PlatformUri -> withContext(Dispatchers.IO) {
        try {
            val androidContext = context as? Context
                ?: throw IllegalArgumentException("Android Context required for URI loading")
            val contentResolver = androidContext.contentResolver
            val inputStream = contentResolver.openInputStream(Uri.parse(source.value))
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            bitmap?.prepareToDraw()
            bitmap?.asImageBitmap()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

actual fun encodeImageBitmapToPng(image: ImageBitmap): ByteArray {
    val bmp = image.asAndroidBitmap()
    val baos = ByteArrayOutputStream()
    bmp.compress(Bitmap.CompressFormat.PNG, 100, baos)
    return baos.toByteArray()
}

actual suspend fun getDominantColorFromUrl(
    url: String,
    context: PlatformContext,
): Color {
    val imageBitmap = loadImageBitmap(ImageSource.Url(url), context)
        ?: return Color.Black

    val androidBitmap = imageBitmap.asAndroidBitmap()

    val p = Palette.from(androidBitmap)
        .maximumColorCount(24)
        .generate()

    val argb = p.dominantSwatch?.rgb
        ?: p.darkVibrantSwatch?.rgb
        ?: p.darkMutedSwatch?.rgb
        ?: p.mutedSwatch?.rgb?.darken(0.5f)
        ?: p.getDominantColor(0xFF000000.toInt()).darken(0.4f)

    return Color(argb)
}

actual suspend fun getDominantColorFromBitmap(
    bitmap: ImageBitmap,
    context: PlatformContext,
): Color {
    val androidBitmap = bitmap.asAndroidBitmap()

    val p = Palette.from(androidBitmap)
        .maximumColorCount(24)
        .generate()

    val argb = p.dominantSwatch?.rgb
        ?: p.darkVibrantSwatch?.rgb
        ?: p.darkMutedSwatch?.rgb
        ?: p.mutedSwatch?.rgb?.darken(0.5f)
        ?: p.getDominantColor(0xFF000000.toInt()).darken(0.4f)

    return Color(argb)
}

actual fun compressImageToJpeg(bytes: ByteArray, maxDimension: Int, quality: Int): ByteArray {
    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return bytes
    val scaled = if (bitmap.width > maxDimension || bitmap.height > maxDimension) {
        val factor = maxDimension.toFloat() / maxOf(bitmap.width, bitmap.height)
        val newW = (bitmap.width * factor).roundToInt().coerceAtLeast(1)
        val newH = (bitmap.height * factor).roundToInt().coerceAtLeast(1)
        bitmap.scale(newW, newH)
    } else {
        bitmap
    }
    val out = ByteArrayOutputStream()
    scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
    return out.toByteArray()
}

private fun Int.darken(factor: Float): Int {
    val a = android.graphics.Color.alpha(this)
    val r = (android.graphics.Color.red(this) * factor).toInt().coerceIn(0, 255)
    val g = (android.graphics.Color.green(this) * factor).toInt().coerceIn(0, 255)
    val b = (android.graphics.Color.blue(this) * factor).toInt().coerceIn(0, 255)
    return android.graphics.Color.argb(a, r, g, b)
}
