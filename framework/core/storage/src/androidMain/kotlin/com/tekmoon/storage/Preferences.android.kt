package com.tekmoon.storage

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import okio.Path.Companion.toPath
import java.io.File

/**
 * Creates a DataStore-backed [Preferences] for the given [name], storing the file as
 * `<context.filesDir>/<name>.preferences_pb`.
 *
 * Call once per logical store at app startup (typically through DI); the returned instance is
 * cheap to share across the app.
 */
fun createPreferences(context: Context, name: String): Preferences {
    val file = File(context.filesDir, "$name.preferences_pb")
    val dataStore = PreferenceDataStoreFactory.createWithPath(
        produceFile = { file.absolutePath.toPath() }
    )
    return DataStorePreferences(dataStore)
}
