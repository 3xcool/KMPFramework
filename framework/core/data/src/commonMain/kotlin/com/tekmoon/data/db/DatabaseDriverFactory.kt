package com.tekmoon.data.db

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema

/**
 * Platform-agnostic factory that creates a [SqlDriver] for a given database.
 *
 * Each platform provides its own constructor requirements:
 * - **Android** — `DatabaseDriverFactory(context: Context, name: String)`
 * - **iOS / native** — `DatabaseDriverFactory(name: String)`
 * - **JVM / Desktop** — `DatabaseDriverFactory(name: String)`
 *
 * Typical DI wiring (Koin):
 * ```kotlin
 * // androidMain Koin module
 * single { DatabaseDriverFactory(androidContext(), "app.db") }
 *
 * // iosMain / jvmMain Koin module
 * single { DatabaseDriverFactory("app.db") }
 *
 * // commonMain
 * single { AppDatabase(get<DatabaseDriverFactory>().createDriver(AppDatabase.Schema)) }
 * ```
 *
 * The factory intentionally takes no default name — every client chooses its own
 * database file name so multiple databases can coexist in the same app.
 */
expect class DatabaseDriverFactory {
    /**
     * Creates and returns a [SqlDriver] backed by the platform's SQLite implementation.
     *
     * @param schema The generated [SqlSchema] from SQLDelight. Used on Android and iOS
     *               to run migrations automatically. On JVM the schema is applied inside
     *               this call before the driver is returned.
     */
    fun createDriver(schema: SqlSchema<QueryResult.Value<Unit>>): SqlDriver
}
