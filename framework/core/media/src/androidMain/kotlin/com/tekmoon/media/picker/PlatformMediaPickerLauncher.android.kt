package com.tekmoon.media.picker

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
actual fun rememberMediaPickerLauncher(
    mediaPickerTypes: Set<MediaPickerType>,
    selectionLimit: Int,
    returnBytes: Boolean,
    maxBytesToReturn: Long,
    onCancelled: (requestKey: String?) -> Unit,
    onResult: (requestKey: String?, List<PickedMediaData>) -> Unit,
): MediaPickerLauncher {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Hold the last requestKey provided by launch()
    var pendingRequestKey by remember { mutableStateOf<String?>(null) }

    val visualMediaPickerType = when {
        mediaPickerTypes.contains(MediaPickerType.Image) && mediaPickerTypes.contains(MediaPickerType.Video) ->
            ActivityResultContracts.PickVisualMedia.ImageAndVideo
        mediaPickerTypes.contains(MediaPickerType.Video) -> ActivityResultContracts.PickVisualMedia.VideoOnly
        else -> ActivityResultContracts.PickVisualMedia.ImageOnly
    }

    val single = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                onResult(pendingRequestKey, listOf(pickMedia(context, uri, returnBytes, maxBytesToReturn)))
                pendingRequestKey = null
            }
        } else {
            onCancelled(pendingRequestKey)
            pendingRequestKey = null
        }
    }

    val multi = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(
            maxItems = if (selectionLimit > 1) selectionLimit else Int.MAX_VALUE,
        ),
    ) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch {
                val items = uris.map { uri -> pickMedia(context, uri, returnBytes, maxBytesToReturn) }
                onResult(pendingRequestKey, items)
                pendingRequestKey = null
            }
        } else {
            onCancelled(pendingRequestKey)
            pendingRequestKey = null
        }
    }

    return remember {
        MediaPickerLauncher(
            onLaunch = { requestKey ->
                pendingRequestKey = requestKey
                val req = PickVisualMediaRequest(visualMediaPickerType)
                if (selectionLimit == 1) single.launch(req) else multi.launch(req)
            },
        )
    }
}

private suspend fun pickMedia(
    context: Context,
    uri: Uri,
    returnBytes: Boolean,
    maxBytesToReturn: Long,
): PickedMediaData = withContext(Dispatchers.IO) {
    val contentResolver = context.contentResolver
    val mime = contentResolver.getType(uri)
    var name: String? = null
    var size: Long? = null
    contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)
        ?.use { c: Cursor ->
            val nameIdx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIdx = c.getColumnIndex(OpenableColumns.SIZE)
            if (c.moveToFirst()) {
                if (nameIdx != -1) name = c.getString(nameIdx)
                if (sizeIdx != -1) size = if (!c.isNull(sizeIdx)) c.getLong(sizeIdx) else null
            }
        }
    val bytes = if (returnBytes && (size == null || size!! <= maxBytesToReturn)) {
        contentResolver.openInputStream(uri)?.use { it.readBytes() }
    } else null

    var width: Int? = null
    var height: Int? = null
    var duration: Long? = null

    try {
        if (mime?.startsWith("image/") == true) {
            contentResolver.openInputStream(uri)?.use { input ->
                val options = android.graphics.BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                android.graphics.BitmapFactory.decodeStream(input, null, options)
                width = options.outWidth
                height = options.outHeight
            }
        } else if (mime?.startsWith("video/") == true) {
            val retriever = android.media.MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, uri)
                width = retriever
                    .extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                    ?.toIntOrNull()
                height = retriever
                    .extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                    ?.toIntOrNull()
                duration = retriever
                    .extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull()
            } finally {
                try { retriever.release() } catch (_: Exception) {}
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    PickedMediaData(
        uri = uri.toString(),
        bytes = bytes,
        mimeType = mime,
        fileName = name,
        sizeBytes = size,
        width = width,
        height = height,
        durationMs = duration,
    )
}
