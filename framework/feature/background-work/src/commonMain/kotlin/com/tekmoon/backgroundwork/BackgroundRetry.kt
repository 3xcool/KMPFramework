package com.tekmoon.backgroundwork

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Retry configuration for a [BackgroundTask] whose handler returns
 * [BackgroundResult.Failure] with `retriable = true`.
 */
public data class BackgroundRetry(
    val maxAttempts: Int = DEFAULT_MAX_ATTEMPTS,
    val backoff: BackoffStrategy = BackoffStrategy.Exponential(initialDelay = DEFAULT_INITIAL_DELAY),
) {
    public companion object {
        public const val DEFAULT_MAX_ATTEMPTS: Int = 3
        public val DEFAULT_INITIAL_DELAY: Duration = 30.seconds
    }
}

public sealed interface BackoffStrategy {
    public data class Linear(val delay: Duration) : BackoffStrategy
    public data class Exponential(val initialDelay: Duration) : BackoffStrategy
}
