package com.tekmoon.utilities.time

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.ExperimentalTime

/**
 * Exercises the platform actual via the common expect signature. Patterns and locale tags here
 * are restricted to portable CLDR tokens (`y M d H m s`) so the same assertions hold on
 * Android / JVM (SimpleDateFormat) and iOS (NSDateFormatter).
 */
@OptIn(ExperimentalTime::class)
class InstantFormatTest {

    private val instant = LocalDateTime(2026, 5, 30, 12, 34, 56).toInstant(TimeZone.UTC)

    @Test
    fun formats_iso_date_time_in_utc() {
        val formatted = instant.format(
            pattern = "yyyy-MM-dd HH:mm:ss",
            locale = LocaleTag("en-US"),
            timeZone = TimeZone.UTC,
        )
        assertEquals("2026-05-30 12:34:56", formatted)
    }

    @Test
    fun formats_date_only() {
        val formatted = instant.format(
            pattern = "yyyy-MM-dd",
            locale = LocaleTag("en-US"),
            timeZone = TimeZone.UTC,
        )
        assertEquals("2026-05-30", formatted)
    }

    @Test
    fun timezone_shifts_wall_clock() {
        // 12:34 UTC == 21:34 Tokyo on the same calendar day
        val formatted = instant.format(
            pattern = "yyyy-MM-dd HH:mm",
            locale = LocaleTag("en-US"),
            timeZone = TimeZone.of("Asia/Tokyo"),
        )
        assertEquals("2026-05-30 21:34", formatted)
    }

    @Test
    fun system_locale_falls_back_when_tag_is_empty() {
        // Use a strictly numeric pattern that's locale-insensitive in output.
        val formatted = instant.format(
            pattern = "yyyy-MM-dd HH:mm",
            locale = LocaleTag.System,
            timeZone = TimeZone.UTC,
        )
        assertEquals("2026-05-30 12:34", formatted)
    }
}
