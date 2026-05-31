package com.tekmoon.storage

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import okio.Path.Companion.toPath
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

/**
 * Creates a DataStore-backed [Preferences] for the given [name], storing the file as
 * `<NSDocumentDirectory>/<name>.preferences_pb`.
 *
 * Call once per logical store at app startup; the returned instance is cheap to share.
 */
fun createPreferences(name: String): Preferences {
    val documents = NSSearchPathForDirectoriesInDomains(
        NSDocumentDirectory,
        NSUserDomainMask,
        true,
    ).firstOrNull() as? String ?: error("Could not resolve NSDocumentDirectory")
    val dataStore = PreferenceDataStoreFactory.createWithPath(
        produceFile = { "$documents/$name.preferences_pb".toPath() }
    )
    return DataStorePreferences(dataStore)
}
