package com.tekmoon.utilities.time

import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.monthsUntil
import kotlinx.datetime.yearsUntil

/**
 * Calendar-day relative position of a date compared to a reference "today".
 *
 * Pure data — the presentation layer maps these into localized strings via the consuming app's
 * resources (the framework deliberately stays out of platform relative-time APIs, which don't
 * agree across Android / iOS / JVM).
 */
sealed interface RelativeTime {
    data object Today : RelativeTime
    data object Yesterday : RelativeTime
    data object Tomorrow : RelativeTime
    data class DaysAgo(val days: Int) : RelativeTime
    data class InDays(val days: Int) : RelativeTime
    data class WeeksAgo(val weeks: Int) : RelativeTime
    data class InWeeks(val weeks: Int) : RelativeTime
    data class MonthsAgo(val months: Int) : RelativeTime
    data class InMonths(val months: Int) : RelativeTime
    data class YearsAgo(val years: Int) : RelativeTime
    data class InYears(val years: Int) : RelativeTime
}

/**
 * Maps the calendar-day gap between `this` and [now] into a [RelativeTime] bucket.
 *
 * Buckets (by absolute day count):
 * - 0 → [RelativeTime.Today]
 * - 1 → [RelativeTime.Yesterday] / [RelativeTime.Tomorrow]
 * - 2..6 → [RelativeTime.DaysAgo] / [RelativeTime.InDays]
 * - 7..29 → [RelativeTime.WeeksAgo] / [RelativeTime.InWeeks] (floor of days / 7)
 * - 30..364 → [RelativeTime.MonthsAgo] / [RelativeTime.InMonths] (calendar-correct via
 *   [LocalDate.monthsUntil])
 * - ≥ 365 → [RelativeTime.YearsAgo] / [RelativeTime.InYears] (calendar-correct via
 *   [LocalDate.yearsUntil])
 */
fun LocalDate.relative(now: LocalDate): RelativeTime {
    val deltaDays = this.daysUntil(now)  // positive = `this` is in the past relative to `now`
    val absDays = if (deltaDays < 0) -deltaDays else deltaDays
    return when {
        absDays == 0 -> RelativeTime.Today
        deltaDays == 1 -> RelativeTime.Yesterday
        deltaDays == -1 -> RelativeTime.Tomorrow
        absDays in 2..6 ->
            if (deltaDays > 0) RelativeTime.DaysAgo(absDays) else RelativeTime.InDays(absDays)
        absDays in 7..29 -> {
            val weeks = absDays / 7
            if (deltaDays > 0) RelativeTime.WeeksAgo(weeks) else RelativeTime.InWeeks(weeks)
        }
        absDays in 30..364 -> {
            val months = if (deltaDays > 0) this.monthsUntil(now) else now.monthsUntil(this)
            if (deltaDays > 0) RelativeTime.MonthsAgo(months) else RelativeTime.InMonths(months)
        }
        else -> {
            val years = if (deltaDays > 0) this.yearsUntil(now) else now.yearsUntil(this)
            if (deltaDays > 0) RelativeTime.YearsAgo(years) else RelativeTime.InYears(years)
        }
    }
}
