package com.tekmoon.utilities.time

import kotlinx.datetime.TimeZone
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.NSTimeZone
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.timeZoneWithName
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
actual fun Instant.format(
    pattern: String,
    locale: LocaleTag,
    timeZone: TimeZone,
): String {
    val formatter = NSDateFormatter().apply {
        dateFormat = pattern
        if (locale.tag.isNotEmpty()) {
            this.locale = NSLocale(localeIdentifier = locale.tag.replace('-', '_'))
        }
        NSTimeZone.timeZoneWithName(timeZone.id)?.let { this.timeZone = it }
    }
    val seconds = epochSeconds.toDouble() + nanosecondsOfSecond / 1_000_000_000.0
    val date = NSDate.dateWithTimeIntervalSince1970(seconds)
    return formatter.stringFromDate(date)
}
