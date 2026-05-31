package com.tekmoon.backgroundwork

/**
 * The body of work the scheduler runs for a task of a given
 * [BackgroundTask.kind].
 *
 * Implementations are registered against a kind via
 * [BackgroundTaskRegistry.register] before any tasks of that kind are
 * scheduled. Handlers must be suspending and cancellation-aware.
 *
 * Returning [BackgroundResult.Failure] with `retriable = true` lets the
 * scheduler retry per the task's [BackgroundRetry] policy. Throwing
 * [kotlinx.coroutines.CancellationException] is treated as
 * [BackgroundStatus.Cancelled].
 */
public fun interface BackgroundTaskHandler {
    public suspend fun execute(input: Map<String, String>): BackgroundResult
}
