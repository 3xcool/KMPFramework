package com.tekmoon.utilities.time

import kotlinx.datetime.TimeZone
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Formats this [Instant] using a platform date-formatting pattern in the given [locale] and
 * [timeZone].
 *
 * Pattern syntax matches each platform's native formatter
 * (`java.text.SimpleDateFormat` on Android / JVM, `NSDateFormatter` on iOS). The CLDR core
 * (`y M d H m s S`) is portable across all three; platform-specific tokens may differ slightly.
 *
 * @param pattern pattern in the platform's date-formatting syntax (e.g. `"yyyy-MM-dd HH:mm"`)
 * @param locale BCP-47 locale tag; [LocaleTag.System] uses the platform default
 * @param timeZone resolution zone for the wall-clock components
 */
@OptIn(ExperimentalTime::class)
expect fun Instant.format(
    pattern: String,
    locale: LocaleTag = LocaleTag.System,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
): String
