package com.tekmoon.data.db

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.native.NativeSqliteDriver

/**
 * iOS / native actual — backed by [NativeSqliteDriver].
 *
 * SQLDelight's native driver places the database file in the app's Documents
 * directory on iOS. Migrations are executed automatically by the driver on first open.
 *
 * @param name Database file name (e.g. `"app.db"`).
 */
actual class DatabaseDriverFactory(private val name: String) {
    actual fun createDriver(schema: SqlSchema<QueryResult.Value<Unit>>): SqlDriver {
        return NativeSqliteDriver(schema, name)
    }
}
