package com.tekmoon.backgroundwork

/**
 * Outcome of a single attempt of a [BackgroundTaskHandler.execute] call.
 *
 * - [Success] — task finished cleanly; the scheduler marks it
 *   [BackgroundStatus.Succeeded] and stops retrying.
 * - [Failure] — task failed. [retriable] controls whether the scheduler
 *   reschedules (subject to [BackgroundRetry.maxAttempts]). The optional
 *   [cause] is surfaced in [BackgroundStatus.Failed].
 * - [Cancelled] — the task itself recognised cancellation. Returning this
 *   prevents the retry loop. (Coroutine cancellation throws and is handled
 *   the same way internally.)
 */
public sealed interface BackgroundResult {
    public object Success : BackgroundResult
    public data class Failure(val cause: Throwable? = null, val retriable: Boolean = true) : BackgroundResult
    public object Cancelled : BackgroundResult
}
