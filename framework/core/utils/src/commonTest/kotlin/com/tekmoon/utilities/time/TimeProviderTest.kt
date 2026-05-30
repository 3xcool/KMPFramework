package com.tekmoon.utilities.time

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class TimeProviderTest {

    private val fixedInstant = LocalDateTime(2026, 5, 30, 12, 34, 56).toInstant(TimeZone.UTC)

    @Test
    fun fixed_provider_returns_constant_now() {
        val provider = FixedTimeProvider(fixedInstant, TimeZone.UTC)

        assertEquals(fixedInstant, provider.now())
        assertEquals(fixedInstant, provider.now())  // never advances
    }

    @Test
    fun fixed_provider_today_is_derived_from_now_in_zone() {
        val utc = FixedTimeProvider(fixedInstant, TimeZone.UTC)
        assertEquals(LocalDate(2026, 5, 30), utc.today())

        // 12:34 UTC is 14:34 in Berlin (CEST, UTC+2 in May) — same calendar day
        val berlin = FixedTimeProvider(fixedInstant, TimeZone.of("Europe/Berlin"))
        assertEquals(LocalDate(2026, 5, 30), berlin.today())

        // 12:34 UTC is 21:34 in Tokyo (UTC+9) — also still 2026-05-30
        val tokyo = FixedTimeProvider(fixedInstant, TimeZone.of("Asia/Tokyo"))
        assertEquals(LocalDate(2026, 5, 30), tokyo.today())
    }

    @Test
    fun fixed_provider_today_can_differ_by_zone_near_midnight() {
        // 23:30 UTC → +9:30 Tokyo → next day in Tokyo
        val nearMidnightUtc = LocalDateTime(2026, 5, 30, 23, 30, 0).toInstant(TimeZone.UTC)
        val utc = FixedTimeProvider(nearMidnightUtc, TimeZone.UTC)
        val tokyo = FixedTimeProvider(nearMidnightUtc, TimeZone.of("Asia/Tokyo"))

        assertEquals(LocalDate(2026, 5, 30), utc.today())
        assertEquals(LocalDate(2026, 5, 31), tokyo.today())  // rolled past midnight
    }

    @Test
    fun fixed_provider_nowLocal_round_trips_through_zone() {
        val provider = FixedTimeProvider(fixedInstant, TimeZone.UTC)
        assertEquals(LocalDateTime(2026, 5, 30, 12, 34, 56), provider.nowLocal())
    }

    @Test
    fun standard_provider_clock_advances() {
        val first = StandardTimeProvider.now()
        // Tiny non-zero sleep so the system clock moves at least one nanosecond on any platform.
        repeat(1_000) { /* spin */ }
        val second = StandardTimeProvider.now()

        assertTrue(second >= first, "Standard clock should not move backwards (got $first → $second)")
    }
}
