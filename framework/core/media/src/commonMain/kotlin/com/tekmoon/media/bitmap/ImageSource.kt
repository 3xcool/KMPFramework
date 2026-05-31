package com.tekmoon.media.bitmap

/**
 * Source descriptor used by [loadImageBitmap].
 */
sealed class ImageSource {
    data class Url(val value: String) : ImageSource()
    data class Bytes(val value: ByteArray) : ImageSource() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false
            other as Bytes
            return value.contentEquals(other.value)
        }
        override fun hashCode(): Int = value.contentHashCode()
    }
    data class PlatformUri(val value: String) : ImageSource()
}
