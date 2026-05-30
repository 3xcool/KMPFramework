package com.tekmoon.utilities.time

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Abstraction over wall-clock time + time-zone reads so tests can inject a fixed clock/zone
 * without depending on [Clock.System] directly.
 *
 * Production code wires [StandardTimeProvider]; tests pass [FixedTimeProvider] or a bespoke
 * implementation to control `now()` deterministically.
 */
@OptIn(ExperimentalTime::class)
interface TimeProvider {
    val clock: Clock
    val timeZone: TimeZone

    fun now(): Instant = clock.now()
    fun today(): LocalDate = now().toLocalDateTime(timeZone).date
    fun nowLocal(): LocalDateTime = now().toLocalDateTime(timeZone)
}

/** Default [TimeProvider] backed by [Clock.System] and [TimeZone.currentSystemDefault]. */
@OptIn(ExperimentalTime::class)
object StandardTimeProvider : TimeProvider {
    override val clock: Clock get() = Clock.System
    override val timeZone: TimeZone get() = TimeZone.currentSystemDefault()
}

/**
 * Deterministic [TimeProvider] for tests. [fixedNow] is constant; instantiate a new provider per
 * scenario, or wrap it in a mutable holder if you need time to advance during a test.
 */
@OptIn(ExperimentalTime::class)
class FixedTimeProvider(
    private val fixedNow: Instant,
    override val timeZone: TimeZone = TimeZone.UTC,
) : TimeProvider {
    override val clock: Clock = object : Clock {
        override fun now(): Instant = fixedNow
    }
}
