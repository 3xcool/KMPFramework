package com.tekmoon.data.db

import app.cash.sqldelight.ColumnAdapter
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlin.time.Instant

/**
 * SQLDelight [ColumnAdapter] implementations for `kotlinx-datetime` types.
 *
 * Register them when constructing your generated [Database]:
 * ```kotlin
 * val db = AppDatabase(
 *     driver = driver,
 *     userAdapter = User.Adapter(
 *         createdAtAdapter = InstantColumnAdapter,
 *         birthDateAdapter = LocalDateColumnAdapter,
 *     )
 * )
 * ```
 *
 * Storage format:
 * | Type              | SQL type | Stored as                          |
 * |-------------------|----------|------------------------------------|
 * | [Instant]         | INTEGER  | Unix epoch milliseconds (UTC)      |
 * | [LocalDate]       | TEXT     | ISO-8601 date string  (`YYYY-MM-DD`)|
 * | [LocalDateTime]   | TEXT     | ISO-8601 datetime (`YYYY-MM-DDTHH:MM:SS`) |
 */

/**
 * Stores [Instant] as a `Long` column (Unix epoch milliseconds, UTC).
 *
 * Choosing millis over ISO text keeps arithmetic queries (e.g. `WHERE created_at > ?`)
 * fast and index-friendly, and avoids locale / timezone parsing bugs.
 */
object InstantColumnAdapter : ColumnAdapter<Instant, Long> {
    override fun decode(databaseValue: Long): Instant =
        Instant.fromEpochMilliseconds(databaseValue)

    override fun encode(value: Instant): Long =
        value.toEpochMilliseconds()
}

/**
 * Stores [LocalDate] as a `TEXT` column in ISO-8601 format (`YYYY-MM-DD`).
 *
 * ISO-8601 strings sort lexicographically, so date range queries remain correct
 * without any special SQL functions.
 */
object LocalDateColumnAdapter : ColumnAdapter<LocalDate, String> {
    override fun decode(databaseValue: String): LocalDate =
        LocalDate.parse(databaseValue)

    override fun encode(value: LocalDate): String =
        value.toString()
}

/**
 * Stores [LocalDateTime] as a `TEXT` column in ISO-8601 format
 * (`YYYY-MM-DDTHH:MM:SS[.nnnnnnnnn]`).
 *
 * **Note:** [LocalDateTime] carries no timezone information. Prefer [Instant] +
 * [InstantColumnAdapter] for timestamps that must be comparable across time zones.
 * Use [LocalDateTime] only for "wall clock" values where the timezone is implicit
 * (e.g., a user-entered appointment time).
 */
object LocalDateTimeColumnAdapter : ColumnAdapter<LocalDateTime, String> {
    override fun decode(databaseValue: String): LocalDateTime =
        LocalDateTime.parse(databaseValue)

    override fun encode(value: LocalDateTime): String =
        value.toString()
}
