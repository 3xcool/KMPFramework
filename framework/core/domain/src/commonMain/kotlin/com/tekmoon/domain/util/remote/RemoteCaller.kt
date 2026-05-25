package com.tekmoon.domain.util.remote

import com.tekmoon.domain.util.data.DataError
import com.tekmoon.domain.util.data.Result
import kotlinx.coroutines.delay
import kotlin.math.min
import kotlin.time.Duration

// ─── Retryable errors ────────────────────────────────────────────────────────

/**
 * Returns `true` for transient network / server errors that are safe to retry.
 *
 * Non-retryable examples: [DataError.Remote.UNAUTHORIZED], [DataError.Remote.FORBIDDEN],
 * [DataError.Remote.NOT_FOUND] — these will not resolve by retrying the same request.
 */
val DataError.Remote.isRetryable: Boolean
    get() = when (this) {
        DataError.Remote.NO_INTERNET,
        DataError.Remote.REQUEST_TIMEOUT,
        DataError.Remote.SERVER_ERROR,
        DataError.Remote.SERVICE_UNAVAILABLE,
        DataError.Remote.TOO_MANY_REQUESTS -> true
        else -> false
    }

// ─── withRetry ───────────────────────────────────────────────────────────────

/**
 * Executes [call] and retries on failure according to [policy].
 *
 * Retry loop:
 * 1. Execute [call].
 * 2. On [Result.Success] → return immediately.
 * 3. On [Result.Failure]:
 *    a. If [RetryPolicy.shouldRetry] returns `false` → return the failure immediately.
 *    b. If all attempts are exhausted → return the last failure.
 *    c. Otherwise → invoke [RetryPolicy.onRetry] (if set), wait for the computed
 *       exponential-back-off delay, then retry.
 *
 * Delay formula: `min(initialDelay * delayFactor^attempt, maxDelay)`
 * where `attempt` is 0-indexed (so the first retry uses `initialDelay * delayFactor^0 = initialDelay`).
 *
 * @param policy Retry configuration. Defaults to [RetryPolicy] with 3 retries and 1 s initial delay.
 * @param call   The suspending remote call to execute.
 * @return The first [Result.Success], or the last [Result.Failure] if all retries are exhausted.
 */
suspend fun <T> withRetry(
    policy: RetryPolicy = RetryPolicy(),
    call: suspend () -> Result<T, DataError.Remote>,
): Result<T, DataError.Remote> {
    var attempt = 0
    while (true) {
        when (val result = call()) {
            is Result.Success -> return result
            is Result.Failure -> {
                val error = result.error
                val isLastAttempt = attempt >= policy.maxRetries
                if (isLastAttempt || !policy.shouldRetry(error)) {
                    return result
                }
                val delayMs = computeDelay(
                    attempt = attempt,
                    initialDelay = policy.initialDelay,
                    delayFactor = policy.delayFactor,
                    maxDelay = policy.maxDelay,
                )
                policy.onRetry?.invoke(attempt + 1, policy.maxRetries, error, delayMs)
                delay(delayMs)
                attempt++
            }
        }
    }
}

// ─── Internal helpers ─────────────────────────────────────────────────────────

private fun computeDelay(
    attempt: Int,
    initialDelay: Duration,
    delayFactor: Double,
    maxDelay: Duration,
): Duration {
    val raw = initialDelay * delayFactor.pow(attempt)
    return if (raw > maxDelay) maxDelay else raw
}

/** Raises [this] to [exponent] using repeated multiplication (avoids `Math.pow` on all platforms). */
private fun Double.pow(exponent: Int): Double {
    if (exponent == 0) return 1.0
    var result = 1.0
    repeat(exponent) { result *= this }
    return result
}
