@file:Suppress("LongMethod")
// rememberCameraLauncher composes Activity-result launchers, camera/video
// branches, FileProvider setup, and cleanup callbacks into a single state
// holder. Splitting it would scatter the launcher bookkeeping that's clearer
// kept together.

package com.tekmoon.media.camera

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.tekmoon.media.picker.PickedMediaData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
actual fun rememberCameraLauncher(
    cameraType: CameraType,
    cameraMode: CameraMode,
    returnBytes: Boolean,
    maxBytesToReturn: Long,
    onResult: (requestKey: String?, PickedMediaData?) -> Unit,
): CameraLauncher {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var pendingRequestKey by rememberSaveable { mutableStateOf<String?>(null) }
    var tempFilePath by rememberSaveable { mutableStateOf<String?>(null) }
    var tempUriStr by rememberSaveable { mutableStateOf<String?>(null) }

    val photoLauncher = rememberLauncherForActivityResult(
        contract = TakePictureWithFacing(cameraType),
    ) { success ->
        scope.launch {
            val file = tempFilePath?.let(::File)
            val uri = tempUriStr?.let(Uri::parse)

            if (success && file != null && uri != null) {
                delay(100)
                if (file.exists() && file.length() > 0) {
                    val result = processMediaFile(
                        context = context,
                        file = file,
                        uri = uri,
                        returnBytes = returnBytes,
                        maxBytesToReturn = maxBytesToReturn,
                        cameraType = cameraType,
                    )
                    onResult(pendingRequestKey, result)
                    pendingRequestKey = null
                } else {
                    file.delete()
                    onResult(pendingRequestKey, null)
                    pendingRequestKey = null
                }
            } else {
                file?.delete()
                onResult(pendingRequestKey, null)
                pendingRequestKey = null
            }
            tempFilePath = null
            tempUriStr = null
        }
    }

    val captureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CaptureVideo(),
    ) { success ->
        scope.launch {
            val file = tempFilePath?.let(::File)
            val uri = tempUriStr?.let(Uri::parse)
            if (success && file != null && uri != null) {
                delay(100)
                if (file.exists() && file.length() > 0) {
                    val result = processMediaFile(
                        context = context,
                        file = file,
                        uri = uri,
                        returnBytes = returnBytes,
                        maxBytesToReturn = maxBytesToReturn,
                        cameraType = cameraType,
                    )
                    onResult(pendingRequestKey, result)
                    pendingRequestKey = null
                } else {
                    file.delete()
                    onResult(pendingRequestKey, null)
                    pendingRequestKey = null
                }
            } else {
                file?.delete()
                onResult(pendingRequestKey, null)
                pendingRequestKey = null
            }
            tempFilePath = null
            tempUriStr = null
        }
    }

    return remember(cameraType, cameraMode) {
        CameraLauncher(
            onLaunch = { requestKey, fileProviderAuthority ->
                pendingRequestKey = requestKey
                try {
                    when (cameraMode) {
                        CameraMode.Photo -> {
                            val file = createTempImageFile(context)
                            val uri = FileProvider.getUriForFile(context, fileProviderAuthority, file)
                            tempFilePath = file.absolutePath
                            tempUriStr = uri.toString()
                            photoLauncher.launch(uri)
                        }
                        CameraMode.Video -> {
                            val file = createTempVideoFile(context)
                            val uri = FileProvider.getUriForFile(context, fileProviderAuthority, file)
                            tempFilePath = file.absolutePath
                            tempUriStr = uri.toString()
                            captureLauncher.launch(uri)
                        }
                    }
                } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                    // Camera capture launch can fail with FileProvider misconfig,
                    // SecurityException from the OS, or activity-not-found. We
                    // log the stack trace and report a null result to the caller.
                    e.printStackTrace()
                    onResult(pendingRequestKey, null)
                    pendingRequestKey = null
                }
            },
        )
    }
}

/** [ActivityResultContracts.TakePicture] variant that hints the front camera when requested. */
private class TakePictureWithFacing(private val cameraType: CameraType) :
    ActivityResultContract<Uri, Boolean>() {
    override fun createIntent(context: Context, input: Uri): Intent =
        Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            .putExtra(MediaStore.EXTRA_OUTPUT, input)
            .apply {
                if (cameraType == CameraType.Front) {
                    // Widely-supported extras for front camera hint (best-effort; OEM-dependent)
                    putExtra("android.intent.extras.CAMERA_FACING", 1)
                    putExtra("android.intent.extras.LENS_FACING_FRONT", 1)
                    putExtra("android.intent.extra.USE_FRONT_CAMERA", true)
                }
            }

    override fun parseResult(resultCode: Int, intent: Intent?): Boolean =
        resultCode == Activity.RESULT_OK
}

private fun createTempImageFile(context: Context): File {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val storageDir = File(context.cacheDir, "camera_images").apply {
        if (!exists()) mkdirs()
    }
    return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
}

private fun createTempVideoFile(context: Context): File {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val storageDir = File(context.cacheDir, "camera_videos").apply {
        if (!exists()) mkdirs()
    }
    return File.createTempFile("MP4_${timeStamp}_", ".mp4", storageDir)
}

private suspend fun processMediaFile(
    context: Context,
    file: File,
    uri: Uri,
    returnBytes: Boolean,
    maxBytesToReturn: Long,
    cameraType: CameraType,
): PickedMediaData? = withContext(Dispatchers.IO) {
    try {
        val mimeType = context.contentResolver.getType(uri) ?: guessMimeType(file.name)

        val isFront = cameraType == CameraType.Front

        // Android mirrors the image (only photos)
        val normalizedFile = if (mimeType.startsWith("image/")) {
            withContext(Dispatchers.IO) {
                normalizeJpegOrientation(src = file, unmirrorFront = isFront)
            }
        } else {
            file
        }
        val sizeBytes = normalizedFile.length()
        val bytes = if (returnBytes && sizeBytes <= maxBytesToReturn) {
            normalizedFile.readBytes()
        } else null

        var width: Int? = null
        var height: Int? = null
        var duration: Long? = null

        if (mimeType.startsWith("image/")) {
            try {
                val options = android.graphics.BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                android.graphics.BitmapFactory.decodeFile(file.absolutePath, options)
                width = options.outWidth
                height = options.outHeight
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                // BitmapFactory.decodeFile can throw OOM or decode errors;
                // missing dimensions are acceptable (caller treats as unknown).
                e.printStackTrace()
            }
        } else if (mimeType.startsWith("video/")) {
            try {
                val retriever = android.media.MediaMetadataRetriever()
                retriever.setDataSource(file.absolutePath)
                width = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull()
                height = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull()
                duration = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
                retriever.release()
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                // MediaMetadataRetriever can throw IllegalArgumentException,
                // RuntimeException, or platform-specific failures; missing
                // metadata is acceptable here.
                e.printStackTrace()
            }
        }

        PickedMediaData(
            uri = uri.toString(),
            bytes = bytes,
            mimeType = mimeType,
            fileName = file.name,
            sizeBytes = sizeBytes,
            width = width,
            height = height,
            durationMs = duration,
        )
    } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
        // processMediaFile orchestrates file I/O, content-resolver lookups,
        // bitmap normalization, and metadata extraction — too many exception
        // sources to enumerate. Failures fall back to null.
        e.printStackTrace()
        null
    }
}

private fun guessMimeType(fileName: String): String = when {
    fileName.endsWith(".jpg", ignoreCase = true) ||
        fileName.endsWith(".jpeg", ignoreCase = true) -> "image/jpeg"
    fileName.endsWith(".png", ignoreCase = true) -> "image/png"
    fileName.endsWith(".mp4", ignoreCase = true) -> "video/mp4"
    fileName.endsWith(".mov", ignoreCase = true) -> "video/quicktime"
    else -> "application/octet-stream"
}
