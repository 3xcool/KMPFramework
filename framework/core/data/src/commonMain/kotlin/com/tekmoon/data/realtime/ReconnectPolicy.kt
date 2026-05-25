package com.tekmoon.data.realtime

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Controls how (and whether) [RealtimeClient] reconnects after a disconnection.
 *
 * ### Choosing a policy
 * - Use [ExponentialBackoff] (the default) for production — it avoids thundering-herd
 *   storms when many clients lose connectivity simultaneously.
 * - Use [Fixed] for simple scenarios or tests where predictable timing matters.
 * - Use [Never] for fire-and-forget connections that must not retry.
 * - Use [Custom] when you need full control (e.g. circuit-breaker patterns).
 */
sealed interface ReconnectPolicy {

    /**
     * No reconnection. The client enters [ConnectionState.Disconnected] immediately
     * after the first failure.
     */
    data object Never : ReconnectPolicy

    /**
     * Doubles the wait time after each failed attempt, capped at [maxDelay].
     *
     * With jitter enabled (the default), each delay is randomised in the range
     * `[delay * 0.5, delay]` to spread reconnect storms across a fleet of clients.
     *
     * @param initialDelay  Delay before the first retry. Defaults to 1 second.
     * @param maxDelay      Upper bound on the delay. Defaults to 30 seconds.
     * @param multiplier    Growth factor per attempt. Defaults to 2.0 (doubles each time).
     * @param maxAttempts   Maximum number of retries. Defaults to [Int.MAX_VALUE] (infinite).
     * @param jitter        Whether to add random jitter. Defaults to `true`.
     */
    data class ExponentialBackoff(
        val initialDelay: Duration = 1.seconds,
        val maxDelay: Duration = 30.seconds,
        val multiplier: Double = 2.0,
        val maxAttempts: Int = Int.MAX_VALUE,
        val jitter: Boolean = true,
    ) : ReconnectPolicy

    /**
     * Waits the same [delay] between every attempt.
     *
     * @param delay       How long to wait between retries.
     * @param maxAttempts Maximum number of retries. Defaults to [Int.MAX_VALUE] (infinite).
     */
    data class Fixed(
        val delay: Duration,
        val maxAttempts: Int = Int.MAX_VALUE,
    ) : ReconnectPolicy

    /**
     * Fully custom policy. Return the [Duration] to wait before the next attempt,
     * or `null` to stop retrying.
     *
     * @param nextDelay Lambda receiving the 1-based attempt number.
     *                  Return `null` to give up; return a [Duration] to schedule the retry.
     */
    data class Custom(
        val nextDelay: suspend (attempt: Int) -> Duration?,
    ) : ReconnectPolicy
}
