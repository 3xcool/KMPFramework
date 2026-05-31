package com.tekmoon.storage

import kotlinx.coroutines.flow.Flow

/**
 * Reactive key-value store backed by `androidx.datastore-preferences-core`.
 *
 * Reads return a [Flow] that emits the current value plus every subsequent update; writes are
 * `suspend` so callers can await persistence.
 *
 * **One type per key.** Keys are name-based — reusing the same key name with a different type
 * overwrites the previous value, and reading with the original type then throws
 * `ClassCastException`. Pick a single type per key for its lifetime
 * (e.g. `"user.theme"` is always a `String`, `"user.notifications.count"` is always an `Int`).
 *
 * Construct via the platform-specific `createPreferences(...)` factory in each source set:
 * - **Android** — `createPreferences(context: Context, name: String): Preferences`
 * - **iOS** — `createPreferences(name: String): Preferences`
 * - **JVM** — `createPreferences(name: String, baseDir: java.io.File = tmpdir): Preferences`
 *
 * The file is created lazily on first access (`name.preferences_pb` in the platform's per-app
 * storage directory).
 */
interface Preferences {

    fun getString(key: String, default: String? = null): Flow<String?>
    fun getInt(key: String, default: Int = 0): Flow<Int>
    fun getLong(key: String, default: Long = 0L): Flow<Long>
    fun getBoolean(key: String, default: Boolean = false): Flow<Boolean>
    fun getFloat(key: String, default: Float = 0f): Flow<Float>
    fun getDouble(key: String, default: Double = 0.0): Flow<Double>
    fun getStringSet(key: String, default: Set<String> = emptySet()): Flow<Set<String>>

    suspend fun putString(key: String, value: String)
    suspend fun putInt(key: String, value: Int)
    suspend fun putLong(key: String, value: Long)
    suspend fun putBoolean(key: String, value: Boolean)
    suspend fun putFloat(key: String, value: Float)
    suspend fun putDouble(key: String, value: Double)
    suspend fun putStringSet(key: String, value: Set<String>)

    /** Removes [key] regardless of its stored type. No-op if the key does not exist. */
    suspend fun remove(key: String)

    /** Clears every key in the store. */
    suspend fun clear()

    /** True if any typed variant of [key] is currently stored. */
    suspend fun contains(key: String): Boolean

    /** Snapshot of every key name currently stored, re-emitted on every change. */
    val keys: Flow<Set<String>>
}
