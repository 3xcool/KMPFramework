package com.tekmoon.utilities.time

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class RelativeTimeTest {

    private val today = LocalDate(2026, 5, 30)

    @Test fun same_day_is_Today() {
        assertEquals(RelativeTime.Today, today.relative(today))
    }

    @Test fun one_day_in_past_is_Yesterday() {
        assertEquals(RelativeTime.Yesterday, LocalDate(2026, 5, 29).relative(today))
    }

    @Test fun one_day_in_future_is_Tomorrow() {
        assertEquals(RelativeTime.Tomorrow, LocalDate(2026, 5, 31).relative(today))
    }

    @Test fun two_to_six_days_in_past_are_DaysAgo() {
        assertEquals(RelativeTime.DaysAgo(2), LocalDate(2026, 5, 28).relative(today))
        assertEquals(RelativeTime.DaysAgo(6), LocalDate(2026, 5, 24).relative(today))
    }

    @Test fun two_to_six_days_in_future_are_InDays() {
        assertEquals(RelativeTime.InDays(2), LocalDate(2026, 6, 1).relative(today))
        assertEquals(RelativeTime.InDays(6), LocalDate(2026, 6, 5).relative(today))
    }

    @Test fun seven_days_past_is_one_week() {
        assertEquals(RelativeTime.WeeksAgo(1), LocalDate(2026, 5, 23).relative(today))
    }

    @Test fun thirteen_days_past_still_one_week_via_floor() {
        // 13 / 7 = 1 (floor division)
        assertEquals(RelativeTime.WeeksAgo(1), LocalDate(2026, 5, 17).relative(today))
    }

    @Test fun fourteen_days_past_is_two_weeks() {
        assertEquals(RelativeTime.WeeksAgo(2), LocalDate(2026, 5, 16).relative(today))
    }

    @Test fun twentynine_days_past_is_four_weeks() {
        // 29 / 7 = 4 (floor)
        assertEquals(RelativeTime.WeeksAgo(4), LocalDate(2026, 5, 1).relative(today))
    }

    @Test fun thirty_days_past_is_one_month() {
        // 2026-04-30 → 2026-05-30 = 1 calendar month (30 days here)
        assertEquals(RelativeTime.MonthsAgo(1), LocalDate(2026, 4, 30).relative(today))
    }

    @Test fun eleven_months_past_via_calendar_units() {
        assertEquals(RelativeTime.MonthsAgo(11), LocalDate(2025, 6, 30).relative(today))
    }

    @Test fun one_year_past() {
        assertEquals(RelativeTime.YearsAgo(1), LocalDate(2025, 5, 30).relative(today))
    }

    @Test fun ten_years_future() {
        assertEquals(RelativeTime.InYears(10), LocalDate(2036, 5, 30).relative(today))
    }

    @Test fun future_weeks_bucket() {
        assertEquals(RelativeTime.InWeeks(2), LocalDate(2026, 6, 13).relative(today))
    }

    @Test fun future_months_bucket() {
        assertEquals(RelativeTime.InMonths(3), LocalDate(2026, 8, 30).relative(today))
    }
}
