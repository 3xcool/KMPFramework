package com.tekmoon.media.camera

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Build
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream

internal fun normalizeJpegOrientation(
    src: File,
    unmirrorFront: Boolean = false, // pass true when cameraType == Front
): File {
    val exif = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        ExifInterface(src)
    } else {
        return src
    }
    val orientation = exif.getAttributeInt(
        ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL,
    )
    val needsFix = orientation != ExifInterface.ORIENTATION_NORMAL &&
        orientation != ExifInterface.ORIENTATION_UNDEFINED
    if (!needsFix) return src

    val bmp = BitmapFactory.decodeFile(src.absolutePath) ?: return src
    val m = Matrix()

    // Apply EXIF rotation/flip first
    when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> m.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> m.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> m.postRotate(270f)
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> m.postScale(-1f, 1f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> m.postScale(1f, -1f)
        ExifInterface.ORIENTATION_TRANSPOSE -> { m.postRotate(90f); m.postScale(-1f, 1f) }
        ExifInterface.ORIENTATION_TRANSVERSE -> { m.postRotate(270f); m.postScale(-1f, 1f) }
    }

    // If this was a selfie and the EXIF didn't already mirror horizontally, un-mirror it
    val exifAlreadyMirrors =
        orientation == ExifInterface.ORIENTATION_FLIP_HORIZONTAL ||
            orientation == ExifInterface.ORIENTATION_TRANSPOSE ||
            orientation == ExifInterface.ORIENTATION_TRANSVERSE

    if (unmirrorFront && !exifAlreadyMirrors) {
        m.postScale(-1f, 1f)
    }

    val fixed = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)

    // Overwrite the original for simplicity (optional: write to a new temp file)
    FileOutputStream(src).use { out ->
        fixed.compress(Bitmap.CompressFormat.JPEG, 95, out)
    }

    // Reset orientation to NORMAL so future viewers don't rotate again
    exif.setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL.toString())
    exif.saveAttributes()

    return src
}
