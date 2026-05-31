package com.tekmoon.backgroundwork

import kotlin.time.Duration

/**
 * When a [BackgroundTask] should run.
 *
 * - [Immediate] — enqueue and run as soon as constraints allow.
 * - [Delayed] — wait [after] before the first attempt.
 * - [Periodic] — run repeatedly with [every] as the interval. [flex] is an
 *   optional flex window the platform may use to batch periodic work for
 *   battery (WorkManager honors this; the in-memory JVM impl ignores it).
 *   Note: WorkManager enforces a minimum periodic interval of 15 minutes.
 */
public sealed interface BackgroundSchedule {
    public object Immediate : BackgroundSchedule
    public data class Delayed(val after: Duration) : BackgroundSchedule
    public data class Periodic(val every: Duration, val flex: Duration? = null) : BackgroundSchedule
}
