package com.tekmoon.domain.util.remote

import com.tekmoon.domain.util.data.DataError
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Configuration for retry behaviour when a remote call fails.
 *
 * @param maxRetries     Maximum number of retry attempts (not counting the initial call).
 * @param initialDelay   Delay before the first retry.
 * @param delayFactor    Multiplicative back-off factor applied on each successive attempt.
 * @param maxDelay       Upper cap for the computed delay.
 * @param shouldRetry    Predicate that decides whether a given error is worth retrying.
 *                       Defaults to [DataError.Remote.isRetryable].
 * @param onRetry        Optional callback invoked *before* each retry sleep so callers can
 *                       observe progress (logging, analytics, UI updates, …).
 *                       Parameters: attempt (1-indexed), maxRetries, the error that triggered
 *                       the retry, and the delay that will be waited before the next attempt.
 */
data class RetryPolicy(
    val maxRetries: Int = 3,
    val initialDelay: Duration = 1.seconds,
    val delayFactor: Double = 2.0,
    val maxDelay: Duration = 30.seconds,
    val shouldRetry: (DataError.Remote) -> Boolean = { it.isRetryable },
    val onRetry: ((attempt: Int, maxRetries: Int, error: DataError.Remote, nextDelay: Duration) -> Unit)? = null,
) {
    companion object {
        /** A policy that never retries — useful for tests or one-shot calls. */
        val NoRetry: RetryPolicy = RetryPolicy(maxRetries = 0)
    }
}
