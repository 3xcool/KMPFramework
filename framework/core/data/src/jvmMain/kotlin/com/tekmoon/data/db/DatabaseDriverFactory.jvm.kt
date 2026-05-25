package com.tekmoon.data.db

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File

/**
 * JVM / Desktop actual — backed by [JdbcSqliteDriver].
 *
 * The database file is stored in the system's temporary directory
 * (`java.io.tmpdir`). This is intentionally simple for a framework default;
 * consumers that need a persistent, user-facing location should supply their
 * own [DatabaseDriverFactory] implementation.
 *
 * Schema creation / migration is applied inside [createDriver] before the
 * driver is returned, because [JdbcSqliteDriver] does not accept a schema
 * in its constructor.
 *
 * @param name Database file name without path (e.g. `"app.db"`).
 */
actual class DatabaseDriverFactory(private val name: String) {
    actual fun createDriver(schema: SqlSchema<QueryResult.Value<Unit>>): SqlDriver {
        val dbFile = File(System.getProperty("java.io.tmpdir"), name)
        val url = "jdbc:sqlite:${dbFile.absolutePath}"
        val driver = JdbcSqliteDriver(url)

        // JdbcSqliteDriver has no built-in migration support; we must call create/migrate
        // explicitly.  Check if the schema version table exists to decide between a
        // fresh create and an incremental migration.
        val currentVersion = try {
            driver.executeQuery(
                identifier = null,
                sql = "PRAGMA user_version",
                mapper = { cursor ->
                    QueryResult.Value(if (cursor.next().value) cursor.getLong(0) ?: 0L else 0L)
                },
                parameters = 0,
            ).value
        } catch (_: Exception) {
            0L
        }

        if (currentVersion == 0L) {
            schema.create(driver)
            driver.execute(null, "PRAGMA user_version = ${schema.version}", 0)
        } else if (currentVersion < schema.version) {
            schema.migrate(driver, currentVersion, schema.version)
            driver.execute(null, "PRAGMA user_version = ${schema.version}", 0)
        }

        return driver
    }
}
