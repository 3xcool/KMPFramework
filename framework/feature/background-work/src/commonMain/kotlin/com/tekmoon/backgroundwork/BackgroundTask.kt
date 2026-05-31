package com.tekmoon.backgroundwork

import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf

/**
 * A unit of background work to be scheduled.
 *
 * - [id] uniquely identifies one *scheduling call*. Two schedule calls with
 *   the same id are treated as the same task by the scheduler.
 * - [kind] is the logical *task type*. See [BackgroundTaskKind] for the
 *   recommended way to define your app's catalog of kinds (enum, sealed
 *   class, or [StringTaskKind] for ad-hoc cases). The kind drives both:
 *   1. handler dispatch — [BackgroundTaskRegistry] maps `kind.id` → handler.
 *   2. policy grouping — [BackgroundPolicy.Conflate] and [BackgroundPolicy.Queue]
 *      operate on all tasks sharing the same kind.
 * - [input] is a serialisable string map handed to the handler. Restricted to
 *   `Map<String, String>` so it can survive process death on Android
 *   (WorkManager `Data`).
 * - [longRunning] is reserved for a follow-up PR that wires Android
 *   `setForeground`. Currently has no behavioural effect.
 */
public data class BackgroundTask(
    val id: String,
    val kind: BackgroundTaskKind,
    val policy: BackgroundPolicy = BackgroundPolicy.Concurrent,
    val schedule: BackgroundSchedule = BackgroundSchedule.Immediate,
    val retry: BackgroundRetry = BackgroundRetry(),
    val input: ImmutableMap<String, String> = persistentMapOf(),
    val requiresNetwork: Boolean = false,
    val requiresCharging: Boolean = false,
    val longRunning: Boolean = false,
)
