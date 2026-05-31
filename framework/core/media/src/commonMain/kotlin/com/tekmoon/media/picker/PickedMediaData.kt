package com.tekmoon.media.picker

data class PickedMediaData(
    val uri: String?,          // "content://", "file://", or platform temp path
    val bytes: ByteArray?,     // present if returnBytes && size <= maxBytesToReturn
    val mimeType: String?,
    val fileName: String?,
    val sizeBytes: Long?,
    val width: Int?,           // image/video dimensions if available
    val height: Int?,
    val durationMs: Long?,     // video duration in milliseconds (null for photos)
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as PickedMediaData

        if (sizeBytes != other.sizeBytes) return false
        if (width != other.width) return false
        if (height != other.height) return false
        if (durationMs != other.durationMs) return false
        if (uri != other.uri) return false
        if (!bytes.contentEquals(other.bytes)) return false
        if (mimeType != other.mimeType) return false
        if (fileName != other.fileName) return false

        return true
    }

    override fun hashCode(): Int {
        var result = sizeBytes?.hashCode() ?: 0
        result = 31 * result + (width ?: 0)
        result = 31 * result + (height ?: 0)
        result = 31 * result + (durationMs?.hashCode() ?: 0)
        result = 31 * result + (uri?.hashCode() ?: 0)
        result = 31 * result + (bytes?.contentHashCode() ?: 0)
        result = 31 * result + (mimeType?.hashCode() ?: 0)
        result = 31 * result + (fileName?.hashCode() ?: 0)
        return result
    }
}
