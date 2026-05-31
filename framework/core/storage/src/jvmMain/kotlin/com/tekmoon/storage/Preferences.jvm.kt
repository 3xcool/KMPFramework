package com.tekmoon.storage

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import okio.Path.Companion.toPath
import java.io.File

/**
 * Creates a DataStore-backed [Preferences] for the given [name], storing the file as
 * `<baseDir>/<name>.preferences_pb`. Defaults to the JVM temp directory; production callers
 * should override [baseDir] with something stable like `~/.<app>/preferences`.
 */
fun createPreferences(
    name: String,
    baseDir: File = File(System.getProperty("java.io.tmpdir")),
): Preferences {
    val file = File(baseDir, "$name.preferences_pb")
    val dataStore = PreferenceDataStoreFactory.createWithPath(
        produceFile = { file.absolutePath.toPath() }
    )
    return DataStorePreferences(dataStore)
}
