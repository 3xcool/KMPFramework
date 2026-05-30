package com.tekmoon.utilities.time

import kotlinx.datetime.TimeZone
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import java.util.TimeZone as JavaTimeZone

@OptIn(ExperimentalTime::class)
actual fun Instant.format(
    pattern: String,
    locale: LocaleTag,
    timeZone: TimeZone,
): String {
    val javaLocale = if (locale.tag.isEmpty()) Locale.getDefault() else Locale.forLanguageTag(locale.tag)
    val formatter = SimpleDateFormat(pattern, javaLocale).apply {
        this.timeZone = JavaTimeZone.getTimeZone(timeZone.id)
    }
    return formatter.format(Date(this.toEpochMilliseconds()))
}
