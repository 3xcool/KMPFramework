package com.tekmoon.data.db

import android.content.Context
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

/**
 * Android actual — backed by [AndroidSqliteDriver].
 *
 * The driver passes [schema] to `AndroidOpenHelper` so SQLite migrations are
 * handled automatically when the on-disk version lags behind the compiled schema.
 *
 * @param context Application [Context]. Typically injected from the Koin Android context.
 * @param name    Database file name (e.g. `"app.db"`).
 */
actual class DatabaseDriverFactory(
    private val context: Context,
    private val name: String,
) {
    actual fun createDriver(schema: SqlSchema<QueryResult.Value<Unit>>): SqlDriver {
        return AndroidSqliteDriver(schema, context, name)
    }
}
